#!/usr/bin/env python3
"""배포된 백엔드를 대상으로 신규 사용자의 전체 여정을 이어서 검증한다.

개별 엔드포인트는 각자 테스트했지만, 앞 단계의 응답값(토큰·roomId·diaryId)을 다음
단계가 실제로 받아 쓰는 흐름은 따로 돌려본 적이 없다. 도메인 경계에서 어긋나는 곳과
배포 환경에서만 나는 문제를 잡는 것이 목적이다.

실행:
    python infra/integration-test.py
    python infra/integration-test.py --base https://drawing-diary-production.up.railway.app

성질:
  * 매 실행마다 integ.{타임스탬프}@example.com 계정을 새로 만들어 반복 실행이 가능하다.
    integ. 로 시작하는 계정과 거기 딸린 데이터는 <b>전부 이 스크립트가 만든 것</b>이다.
  * 실패해도 멈추지 않는다. 뒤 단계가 앞 단계의 결과를 못 받아 진행 불가일 때만 건너뛴다.
  * <b>전부 PASS일 때만</b> 남긴 데이터를 DB에서 지운다(탈퇴 API가 아니라 실제 DELETE).
    하나라도 실패하면 원인을 DB에서 들여다볼 수 있도록 그대로 둔다.

DB 정리에는 연결 문자열이 필요하다. 자격 증명을 파일에 남기지 않으려고 인자나
환경 변수로만 받는다. 주지 않으면 정리를 건너뛰고 그 사실을 report에 적는다.
    --db-url "postgresql://..."   또는   환경변수 RAILWAY_DB_URL
"""

import argparse
import base64
import datetime
import io
import json
import os
import struct
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import zlib

DEFAULT_BASE = "https://drawing-diary-production.up.railway.app"

# AI 서버(Render 무료 티어)는 유휴 상태에서 깨어나는 데만 수십 초가 걸린다(실측 52초).
# 백엔드의 read-timeout이 120초라 그보다 넉넉하게 잡아야 클라이언트가 먼저 끊지 않는다.
AI_TIMEOUT = 300
DEFAULT_TIMEOUT = 30

# 활동 잔디는 app.time-zone.display(기본 Asia/Seoul) 기준으로 묶인다. createdAt은 UTC로
# 내려오므로 기대값을 그쪽으로 옮길 때 쓴다. 서울은 서머타임이 없어 고정 오프셋으로 충분하다.
KST_OFFSET = datetime.timedelta(hours=9)

RESULTS = []          # (step, label, ok, detail)
FAILURES = []         # 실패 상세 — 요청/응답 전문
NOTES = []            # PASS/FAIL로 가를 일은 아니지만 사람이 알아야 하는 관찰
CREATED = {           # 정리 대상. 실패 시 report에 그대로 싣는다.
    "users": [], "rooms": [], "diaries": [], "images": [], "categories": [],
    "comments": [], "tags": [],
}
_step = {"name": "-"}


# ────────────────────────────── HTTP ──────────────────────────────

def call(method, path, token=None, body=None, params=None, timeout=DEFAULT_TIMEOUT,
         raw_body=None, content_type=None):
    """(status, parsed_body) 를 돌려준다. 4xx/5xx도 예외가 아니라 값으로 받는다."""
    url = path if path.startswith("http") else BASE + path
    if params:
        url += "?" + urllib.parse.urlencode(params, encoding="utf-8")

    data = raw_body
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        content_type = content_type or "application/json; charset=utf-8"

    req = urllib.request.Request(url, data=data, method=method)
    if token:
        req.add_header("Authorization", "Bearer " + token)
    if content_type:
        req.add_header("Content-Type", content_type)

    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, _parse(r.read(), r.headers.get("Content-Type", ""))
    except urllib.error.HTTPError as e:
        return e.code, _parse(e.read(), e.headers.get("Content-Type", ""))
    except Exception as e:                                    # 타임아웃·연결 실패
        return 0, {"__transport_error__": f"{type(e).__name__}: {e}"}


def _parse(raw, content_type):
    if not raw:
        return None
    if "image/" in content_type:
        return {"__bytes__": len(raw), "__magic__": raw[:8].hex()}
    text = raw.decode("utf-8", "replace")
    try:
        return json.loads(text)
    except ValueError:
        return text[:400]


def upload_image(token, name="integ.png"):
    """multipart/form-data 는 stdlib에 빌더가 없어 직접 만든다. 파트 이름은 file."""
    boundary = "----integ" + str(int(time.time() * 1000))
    png = make_png()
    body = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="{name}"\r\n'
        f"Content-Type: image/png\r\n\r\n"
    ).encode() + png + f"\r\n--{boundary}--\r\n".encode()
    return call("POST", "/api/images", token, raw_body=body,
                content_type=f"multipart/form-data; boundary={boundary}")


def make_png(w=96, h=96, color=(206, 132, 92)):
    """의존성 없이 유효한 PNG를 만든다. 1x1은 AI 서버가 거부할 수 있어 96x96으로 만든다."""
    raw = b"".join(b"\x00" + bytes(color) * w for _ in range(h))

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw))
            + chunk(b"IEND", b""))


# ────────────────────────── 결과 기록 ──────────────────────────

def step(name):
    _step["name"] = name
    print(f"\n{'━' * 74}\n{name}\n{'━' * 74}")


def check(label, ok, detail="", request=None, response=None):
    RESULTS.append((_step["name"], label, bool(ok), str(detail)[:300]))
    mark = "PASS" if ok else "FAIL"
    print(f"  [{mark}] {label}" + (f"  — {detail}" if detail else ""))
    if not ok:
        FAILURES.append({
            "step": _step["name"], "label": label, "detail": str(detail),
            "request": request, "response": _redact(response),
        })
    return bool(ok)


def note(text):
    """맞고 틀림의 문제가 아니라 '알아둬야 하는 사실'. 리포트에 따로 모은다."""
    NOTES.append((_step["name"], text))
    print(f"  [NOTE] {text}")


IMAGE_MAGIC = {
    "89504e47": "PNG", "ffd8ff": "JPEG", "52494646": "WebP(RIFF)", "47494638": "GIF",
}


def image_kind(magic_hex):
    for prefix, name in IMAGE_MAGIC.items():
        if magic_hex.startswith(prefix):
            return name
    return None


def _redact(obj):
    """토큰이 report 파일에 남지 않게 지운다."""
    if isinstance(obj, dict):
        return {k: ("<redacted>" if k in ("accessToken", "refreshToken") else _redact(v))
                for k, v in obj.items()}
    if isinstance(obj, list):
        return [_redact(x) for x in obj]
    return obj


def js(o):
    return json.dumps(o, ensure_ascii=False, default=str)


# ────────────────────────────── 시나리오 ──────────────────────────────

def run():
    stamp = int(time.time())
    A_EMAIL = f"integ.{stamp}.a@example.com"
    B_EMAIL = f"integ.{stamp}.b@example.com"
    C_EMAIL = f"integ.{stamp}.c@example.com"
    PW = "password123"
    TAG = f"integ-{stamp}"
    S = {}   # 단계 간에 넘기는 값

    # ── 1단계: 가입과 로그인 ──────────────────────────────
    step("1단계: 가입과 로그인")
    # C는 방에도 들어오지 않고 아무도 팔로우하지 않는 제3자다. 9단계에서 쓴다 —
    # B는 방 멤버라 발행 시점에 협업자로 복사되므로 공개 범위 판정을 통과해버린다.
    for key, email, nick in [("A", A_EMAIL, f"integA{stamp}"),
                             ("B", B_EMAIL, f"integB{stamp}"),
                             ("C", C_EMAIL, f"integC{stamp}")]:
        st, r = call("POST", "/api/auth/signup", body={"email": email, "password": PW, "nickname": nick})
        # 문서에는 상태 코드가 없지만 실제로는 201이다. 둘 다 성공으로 본다.
        ok = check(f"{key} 회원가입", st in (200, 201) and isinstance(r, dict) and r.get("email") == email,
                   f"HTTP {st} {js(r)}", request={"email": email, "nickname": nick}, response=r)
        if key == "A" and st == 201:
            note("POST /api/auth/signup 은 200이 아니라 **201**을 반환한다. docs/API.md에 상태 코드가 적혀 있지 않다.")
        if ok:
            CREATED["users"].append(email)
        st, r = call("POST", "/api/auth/login", body={"email": email, "password": PW})
        if check(f"{key} 로그인 (accessToken·refreshToken)", st == 200 and r.get("accessToken") and r.get("refreshToken"),
                 f"HTTP {st} userId={r.get('userId') if isinstance(r, dict) else '?'}", response=r):
            S[key] = r["accessToken"]
            S[key + "_refresh"] = r["refreshToken"]
            S[key + "_id"] = r["userId"]
            S[key + "_nick"] = nick

    if "A" not in S or "B" not in S:
        check("이후 단계 진행 가능", False, "로그인 실패로 나머지 단계를 진행할 수 없다")
        return S, TAG

    st, r = call("GET", "/api/users/me", S["A"])
    check("A /api/users/me — 가입 정보와 일치", st == 200 and r.get("email") == A_EMAIL and r.get("id") == S["A_id"],
          f"HTTP {st} {js(r)}", response=r)

    new_bio = "통합테스트 계정입니다"
    new_img = "https://picsum.photos/seed/integ-a/200/200"
    new_nick = f"integA{stamp}수정"
    st, r = call("PATCH", "/api/users/me", S["A"],
                 {"nickname": new_nick, "bio": new_bio, "profileImageUrl": new_img})
    check("A 프로필 수정", st == 200, f"HTTP {st} {js(r)}", response=r)

    st, r = call("GET", "/api/users/me", S["A"])
    check("수정 내용이 재조회에 반영",
          st == 200 and r.get("nickname") == new_nick and r.get("bio") == new_bio
          and r.get("profileImageUrl") == new_img,
          f"HTTP {st} {js(r)}", response=r)
    S["A_nick"] = new_nick

    # ── 2단계: 소셜 ──────────────────────────────
    step("2단계: 소셜 (팔로우 → 목록 → isFollowing → 알림)")
    st, r = call("POST", f"/api/users/{S['B_id']}/follow", S["A"])
    check("A → B 팔로우", st == 200, f"HTTP {st} {js(r)}", response=r)

    st, r = call("GET", f"/api/users/{S['B_id']}/followers", S["A"])
    check("B의 팔로워 목록에 A가 있음",
          st == 200 and any(u.get("userId") == S["A_id"] for u in (r or [])),
          f"HTTP {st} {js(r)}", response=r)

    st, r = call("GET", f"/api/users/{S['B_id']}", S["A"])
    check("A가 본 B 프로필의 isFollowing == true",
          st == 200 and r.get("isFollowing") is True, f"HTTP {st} {js(r)}", response=r)
    check("타인 프로필 응답에 email이 없음 (문서 명시)",
          isinstance(r, dict) and "email" not in r, f"키: {list(r.keys()) if isinstance(r, dict) else r}",
          response=r)

    st, r = call("GET", f"/api/users/{S['A_id']}", S["B"])
    check("B가 본 A 프로필의 isFollowing == false (역방향은 팔로우 아님)",
          st == 200 and r.get("isFollowing") is False, f"HTTP {st} isFollowing={r.get('isFollowing')}",
          response=r)

    st, r = call("GET", "/api/notifications", S["B"])
    follow_noti = [n for n in (r or []) if n.get("type") == "FOLLOW" and n.get("senderId") == S["A_id"]]
    check("B에게 FOLLOW 알림 도착", st == 200 and len(follow_noti) == 1,
          f"HTTP {st} FOLLOW {len(follow_noti)}건 / 전체 {len(r or [])}건", response=r)
    if follow_noti:
        check("FOLLOW 알림의 targetId는 null (문서 명시)", follow_noti[0].get("targetId") is None,
              f"targetId={follow_noti[0].get('targetId')}", response=follow_noti[0])

    # ── 3단계: 카테고리와 방 ──────────────────────────────
    step("3단계: 카테고리와 방")
    for name in ("통합테스트일상", "통합테스트여행"):
        st, r = call("POST", "/api/users/me/categories", S["A"], {"name": name})
        if check(f"A 카테고리 생성 — {name}", st in (200, 201) and r.get("categoryId") or r.get("id"),
                 f"HTTP {st} {js(r)}", response=r):
            CREATED["categories"].append(r.get("categoryId") or r.get("id"))
    S["cat"] = CREATED["categories"][0] if CREATED["categories"] else None

    st, r = call("POST", "/api/rooms", S["A"])
    if check("A 방 생성", st in (200, 201) and r.get("roomId"), f"HTTP {st} {js(r)}", response=r):
        S["room"] = r["roomId"]
        CREATED["rooms"].append(S["room"])

    if "room" in S:
        st, r = call("POST", f"/api/rooms/{S['room']}/invite", S["A"], {"invitedUserIds": [S["B_id"]]})
        check("A가 B를 방에 초대", st == 200, f"HTTP {st} {js(r)}", response=r)

        st, r = call("GET", "/api/notifications", S["B"])
        inv = [n for n in (r or []) if n.get("type") == "ROOM_INVITE" and n.get("targetId") == S["room"]]
        check("B에게 ROOM_INVITE 알림 도착 (targetId == roomId)", st == 200 and len(inv) == 1,
              f"HTTP {st} ROOM_INVITE {len(inv)}건", response=r)

        st, r = call("POST", f"/api/rooms/{S['room']}/join", S["B"])
        check("B 방 참여", st == 200, f"HTTP {st} {js(r)}", response=r)

        st, r = call("GET", f"/api/rooms/{S['room']}", S["A"])
        ids = [m.get("userId") for m in (r or {}).get("members", [])] if isinstance(r, dict) else []
        check("방 멤버가 A, B 둘 다", st == 200 and S["A_id"] in ids and S["B_id"] in ids,
              f"HTTP {st} members={ids}", response=r)

    # ── 4단계: AI 가이드 ──────────────────────────────
    step("4단계: AI 선화 가이드 (AI 서버 콜드스타트로 오래 걸릴 수 있음)")
    if "room" in S:
        t0 = time.time()
        st, r = call("POST", f"/api/rooms/{S['room']}/ai-guide", S["A"],
                     {"text": "비 오는 날 창가에 앉아 따뜻한 차를 마시는 고양이"}, timeout=AI_TIMEOUT)
        elapsed = round(time.time() - t0, 1)
        guides = (r or {}).get("guides") if isinstance(r, dict) else None
        ok = check("ai-guide 200 + guides 3종", st == 200 and isinstance(guides, list) and len(guides) == 3,
                   f"HTTP {st} {elapsed}초 {js(r)[:200]}", response=r)
        if ok:
            succeeded = [g for g in guides if g.get("imageUrl")]
            check(f"가이드 이미지 URL 확보 ({len(succeeded)}/3 성공)", len(succeeded) > 0,
                  f"실패: {[g.get('error') for g in guides if not g.get('imageUrl')]}", response=guides)
            kinds = set()
            for g in succeeded:
                st2, r2 = call("GET", g["imageUrl"], timeout=60)
                kind = image_kind(r2.get("__magic__", "")) if isinstance(r2, dict) else None
                kinds.add(kind)
                check(f"  가이드 이미지 접근 가능 (200 + 실제 이미지 바이트) — {g.get('styleName')}",
                      st2 == 200 and kind is not None and r2.get("__bytes__", 0) > 1000,
                      f"HTTP {st2} {kind} {r2.get('__bytes__') if isinstance(r2, dict) else '?'}bytes",
                      response=r2)
                # 가이드 3종도 images 테이블에 남는다. 정리를 건너뛴 경우 리포트에
                # 실려야 손으로 지울 수 있으므로 여기서 전부 기록한다.
                if "/api/images/" in g["imageUrl"]:
                    CREATED["images"].append(int(g["imageUrl"].rsplit("/", 1)[-1]))
            if kinds:
                note(f"AI 선화 가이드 이미지의 실제 형식은 {'/'.join(sorted(k for k in kinds if k))}다. "
                     "docs/API.md에 형식이 명시되어 있지 않다(업로드 허용 형식과 별개).")

    # ── 5단계: 작업 저장과 발행 ──────────────────────────────
    step("5단계: 임시 저장 → 복구 → 이미지 업로드 → 발행")
    TITLE = "통합테스트 일기"
    CONTENT = "임시 저장해둔 본문이 발행 때 그대로 딸려와야 한다."
    canvas = base64.b64encode(json.dumps(
        {"version": 1, "strokes": [{"tool": "pencil", "points": [[1, 2], [3, 4]]}]},
        ensure_ascii=False).encode()).decode()

    if "room" in S:
        st, r = call("PUT", f"/api/rooms/{S['room']}/canvas", S["A"],
                     {"canvasData": canvas, "title": TITLE, "content": CONTENT})
        check("A 임시 저장", st == 200, f"HTTP {st} {js(r)}", response=r)

        st, r = call("GET", f"/api/rooms/{S['room']}/canvas", S["B"])
        check("B가 A의 임시 저장분을 복구",
              st == 200 and r.get("title") == TITLE and r.get("content") == CONTENT
              and r.get("canvasData") == canvas,
              f"HTTP {st} title={r.get('title')!r} canvas일치={r.get('canvasData') == canvas}", response=r)

    st, r = upload_image(S["A"])
    if check("완성작 이미지 업로드", st in (200, 201) and isinstance(r, dict) and r.get("url"),
             f"HTTP {st} {js(r)}", response=r):
        S["img_url"] = r["url"]
        # 업로드 URL은 요청이 들어온 호스트로 만들어진다. 프록시 뒤(Railway)에서 https가
        # 유지되는지는 배포 서버에서만 의미가 있어, 로컬(http) 실행에서는 건너뛴다.
        if BASE.startswith("https://"):
            check("업로드 URL이 https (프록시 뒤 스킴 복원)", str(r["url"]).startswith("https://"),
                  r["url"], response=r)
        else:
            note(f"로컬(http) 실행이라 '업로드 URL이 https' 검사는 건너뛴다 — 받은 URL: {r['url']}")
        CREATED["images"].append(int(str(r["url"]).rsplit("/", 1)[-1]))

    if "room" in S:
        submit = {"visibility": "PUBLIC", "tags": [TAG, "일상"]}
        if S.get("cat"):
            submit["categoryId"] = S["cat"]
        if S.get("img_url"):
            submit["finalImg"] = S["img_url"]
        st, r = call("POST", f"/api/rooms/{S['room']}/submit", S["A"], submit)
        if check("발행 (title/content 생략 → 임시저장분 사용)", st == 200 and r.get("diaryId"),
                 f"HTTP {st} {js(r)}", request=submit, response=r):
            S["diary"] = r["diaryId"]
            CREATED["diaries"].append(S["diary"])
            CREATED["tags"].append(TAG)

    if "diary" in S:
        st, r = call("GET", f"/api/diaries/{S['diary']}", S["A"])
        tags = [t.get("name") for t in (r or {}).get("tags", [])] if isinstance(r, dict) else []
        check("발행된 일기의 title/content가 임시저장분과 일치",
              st == 200 and r.get("title") == TITLE and r.get("content") == CONTENT,
              f"HTTP {st} title={r.get('title')!r}", response=r)
        check("카테고리 반영", (S.get("cat") is None) or (r.get("categoryId") == S["cat"]),
              f"categoryId={r.get('categoryId')} (기대 {S.get('cat')})", response=r)
        check("태그 반영", TAG in tags and "일상" in tags, f"tags={tags}", response=r)
        check("임시저장 canvasData가 일기로 복사됨", r.get("canvasData") == canvas,
              f"일치={r.get('canvasData') == canvas}", response=r)

    # ── 6단계: AI 채점과 랭킹 ──────────────────────────────
    step("6단계: AI 채점과 랭킹")
    if "diary" in S:
        t0 = time.time()
        st, r = call("POST", f"/api/diaries/{S['diary']}/ai-score", S["A"], timeout=AI_TIMEOUT)
        elapsed = round(time.time() - t0, 1)
        ok = check("ai-score 200", st == 200 and isinstance(r, dict) and r.get("totalScore") is not None,
                   f"HTTP {st} {elapsed}초 {js(r)}", response=r)
        if ok:
            expected = score_formula(r["relevanceScore"], r["colorScore"], r["likeScore"])
            check("totalScore가 공식과 일치 (rel*0.5 + color*0.3 + likeScore*0.2, 반올림)",
                  r["totalScore"] == expected,
                  f"응답 {r['totalScore']} vs 계산 {expected} "
                  f"(rel={r['relevanceScore']} color={r['colorScore']} like={r['likeScore']})",
                  response=r)
            check("feedback 필드 존재", "feedback" in r,
                  f"feedback={str(r.get('feedback'))[:60]!r}", response=r)
            S["score"] = r

        st, r = call("GET", "/api/rankings", S["A"], params={"limit": 50})
        check("전체 랭킹에 이 일기가 있음",
              st == 200 and any(x.get("diaryId") == S["diary"] for x in (r or [])),
              f"HTTP {st} 랭킹 {len(r or [])}건", response=r)

        st, r = call("GET", "/api/rankings/me", S["A"])
        check("내 랭킹에 이 일기가 있음",
              st == 200 and any(x.get("diaryId") == S["diary"] for x in (r or [])),
              f"HTTP {st} {js(r)}", response=r)

    # ── 7단계: 상호작용 ──────────────────────────────
    step("7단계: 좋아요 · 댓글 · 알림")
    if "diary" in S:
        st, r = call("POST", f"/api/diaries/{S['diary']}/likes", S["B"])
        check("B 좋아요", st == 200 and r.get("liked") is True and r.get("likeCount") == 1,
              f"HTTP {st} {js(r)}", response=r)

        st, r = call("GET", f"/api/diaries/{S['diary']}/scores", S["A"])
        if check("좋아요 후 점수 재조회", st == 200, f"HTTP {st} {js(r)}", response=r):
            check("좋아요 반영되어 likeScore == 5", r.get("likeScore") == 5,
                  f"likeScore={r.get('likeScore')}", response=r)
            check("totalScore가 재계산됨 (공식 일치)",
                  r.get("totalScore") == score_formula(r["relevanceScore"], r["colorScore"], r["likeScore"]),
                  f"total={r.get('totalScore')} 기대={score_formula(r['relevanceScore'], r['colorScore'], r['likeScore'])}",
                  response=r)
            if S.get("score"):
                check("좋아요 전보다 totalScore가 올라감",
                      r["totalScore"] >= S["score"]["totalScore"],
                      f"{S['score']['totalScore']} → {r['totalScore']}", response=r)

        st, r = call("POST", f"/api/diaries/{S['diary']}/comments", S["B"], {"content": "통합테스트 댓글"})
        if check("B 댓글 작성 (201)", st == 201 and r.get("id"), f"HTTP {st} {js(r)}", response=r):
            S["comment"] = r["id"]
            CREATED["comments"].append(S["comment"])

        if "comment" in S:
            st, r = call("PATCH", f"/api/comments/{S['comment']}", S["B"], {"content": "통합테스트 댓글 수정본"})
            check("B 댓글 수정", st == 200 and r.get("content") == "통합테스트 댓글 수정본",
                  f"HTTP {st} {js(r)}", response=r)

            st, r = call("GET", f"/api/diaries/{S['diary']}/comments", S["A"])
            mine = [c for c in (r or []) if c.get("id") == S["comment"]]
            check("댓글 목록에 수정본이 반영",
                  st == 200 and mine and mine[0].get("content") == "통합테스트 댓글 수정본",
                  f"HTTP {st} {js(mine)}", response=r)

            st, r = call("DELETE", f"/api/comments/{S['comment']}", S["A"])
            check("A가 B의 댓글 삭제 시도 → 403", st == 403, f"HTTP {st} {js(r)}", response=r)

        st, r = call("GET", f"/api/diaries/{S['diary']}/likes", S["A"])
        check("좋아요 목록에 B가 있음",
              st == 200 and any(u.get("userId") == S["B_id"] for u in (r or [])),
              f"HTTP {st} {js(r)}", response=r)

        st, r = call("GET", "/api/notifications", S["A"])
        types = [n.get("type") for n in (r or []) if n.get("targetId") == S["diary"]]
        check("A에게 LIKE 알림 도착", "LIKE" in types, f"HTTP {st} 이 일기 관련 알림: {types}", response=r)
        check("A에게 COMMENT 알림 도착", "COMMENT" in types, f"이 일기 관련 알림: {types}", response=r)

    # ── 8단계: 조회 계열 ──────────────────────────────
    step("8단계: 피드 · 탐색 · 잔디 · 타인 일기 목록")
    if "diary" in S:
        # A가 B를 팔로우할 뿐 B는 A를 팔로우하지 않는다 → B의 피드에는 A의 일기가 없어야 한다.
        st, r = call("GET", "/api/feed", S["B"], params={"limit": 50})
        check("B의 피드에 A의 일기가 없음 (B는 A를 팔로우하지 않음)",
              st == 200 and not any(x.get("id") == S["diary"] for x in (r or [])),
              f"HTTP {st} 피드 {len(r or [])}건", response=r)

        st, r = call("GET", "/api/explore", S["B"], params={"limit": 50})
        check("탐색(PUBLIC)에 A의 일기가 있음",
              st == 200 and any(x.get("id") == S["diary"] for x in (r or [])),
              f"HTTP {st} 탐색 {len(r or [])}건", response=r)

        found = False
        for _ in range(6):                     # 랜덤이라 한 번에 안 걸릴 수 있어 몇 번 뽑아본다
            st, r = call("GET", "/api/explore/random", S["B"], params={"limit": 20})
            if any(x.get("id") == S["diary"] for x in (r or [])):
                found = True
                break
        check("랜덤 추천에서 A의 일기가 뽑힘 (최대 6회 시도)", found, "", response=r)

        # 잔디는 KST(app.time-zone.display) 기준으로 묶인다. createdAt은 UTC로 내려오므로
        # 기대값도 UTC → KST로 옮겨서 잡는다. 실행 환경의 today를 그냥 쓰면 테스트가
        # 실행 머신의 시간대에 딸려가므로 그렇게 하지 않는다.
        st, det = call("GET", f"/api/diaries/{S['diary']}", S["A"])
        stored = str((det or {}).get("createdAt", ""))[:19]
        stored_date = stored[:10]
        kst_date = (datetime.datetime.fromisoformat(stored) + KST_OFFSET).date().isoformat()
        st, r = call("GET", f"/api/users/{S['A_id']}/activity", S["B"],
                     params={"year": int(kst_date[:4]), "month": int(kst_date[5:7])})
        dates = [x.get("date") for x in (r or [])]
        hit = [x for x in (r or []) if x.get("date") == kst_date]
        check("A의 활동 잔디에 일기 작성일이 KST 기준으로 찍힘",
              st == 200 and hit and hit[0].get("count", 0) >= 1,
              f"HTTP {st} 기대 {kst_date} / 응답 {js(r)}", response=r)
        if kst_date != stored_date:
            # KST 00:00~09:00에 실행됐을 때만 갈라지는 구간. 예전에는 여기서 하루 밀렸다.
            check("UTC 날짜 칸에는 찍히지 않음 (하루 밀림 회귀 방지)",
                  stored_date not in dates,
                  f"저장 {stored}(UTC) → 잔디 {kst_date}(KST). UTC칸 {stored_date} 포함여부="
                  f"{stored_date in dates}", response=r)
            note(f"이번 실행은 KST 00:00~09:00 구간이라 UTC 날짜(`{stored_date}`)와 KST 날짜"
                 f"(`{kst_date}`)가 갈렸다. 잔디가 KST 칸에 찍히는 것을 실제로 확인했다.")

        st, r = call("GET", f"/api/users/{S['A_id']}/diaries", S["B"], params={"limit": 20})
        check("B가 본 A의 일기 목록에 이 일기가 있음",
              st == 200 and any(x.get("id") == S["diary"] for x in (r or [])),
              f"HTTP {st} {len(r or [])}건", response=r)
        item = next((x for x in (r or []) if x.get("id") == S["diary"]), None)
        if item:
            check("목록 응답에 tags가 포함 (피드와 같은 형식)",
                  isinstance(item.get("tags"), list) and TAG in [t.get("name") for t in item["tags"]],
                  f"tags={item.get('tags')}", response=item)

    # ── 9단계: 접근 제어 ──────────────────────────────
    step("9단계: 공개 범위별 접근 제어")
    if "diary" in S:
        # 판정 대상이 둘이다. B는 방 멤버였으므로 submit이 협업자로 복사했다 — 공개 범위와
        # 무관하게 항상 볼 수 있다. C는 방에 들어온 적도, A를 팔로우한 적도 없는 제3자다.
        st, r = call("PATCH", f"/api/diaries/{S['diary']}", S["A"], {"visibility": "PRIVATE"})
        check("A가 PRIVATE으로 변경", st == 200, f"HTTP {st} {js(r)}", response=r)

        st, r = call("GET", f"/api/diaries/{S['diary']}", S["C"])
        check("제3자 C 조회 → 403", st == 403, f"HTTP {st} {js(r)}", response=r)

        st, r = call("GET", f"/api/diaries/{S['diary']}", S["B"])
        check("협업자 B는 PRIVATE도 조회 가능 → 200 (발행 시 방 멤버가 협업자로 복사됨)",
              st == 200, f"HTTP {st} {js(r)[:120]}", response=r)
        if st == 200:
            note("9단계의 'B가 조회 → 403' 이라는 시나리오 전제는 성립하지 않는다. B는 3단계에서 "
                 "방에 참여했고 `submit()`이 발행 시점의 방 멤버 전원을 협업자로 복사하므로, "
                 "B는 PRIVATE도 볼 수 있는 **공동 작성자**다. 접근 제어는 제3자 C로 판정한다.")

        st, r = call("GET", "/api/explore", S["C"], params={"limit": 50})
        check("C의 탐색 목록에서 사라짐",
              st == 200 and not any(x.get("id") == S["diary"] for x in (r or [])),
              f"HTTP {st} 탐색 {len(r or [])}건", response=r)

        st, r = call("PATCH", f"/api/diaries/{S['diary']}", S["A"], {"visibility": "FOLLOWERS_ONLY"})
        check("A가 FOLLOWERS_ONLY로 변경", st == 200, f"HTTP {st} {js(r)}", response=r)

        st, r = call("GET", f"/api/diaries/{S['diary']}", S["C"])
        check("C는 A를 팔로우하지 않으므로 여전히 403", st == 403, f"HTTP {st} {js(r)}", response=r)

        st, _ = call("POST", f"/api/users/{S['A_id']}/follow", S["C"])
        check("C → A 팔로우", st == 200, f"HTTP {st}")

        st, r = call("GET", f"/api/diaries/{S['diary']}", S["C"])
        check("팔로우 후 C도 200", st == 200,
              f"HTTP {st} title={r.get('title') if isinstance(r, dict) else r}", response=r)

        st, r = call("GET", "/api/feed", S["C"], params={"limit": 50})
        check("팔로우했으므로 C의 피드에 나타남",
              st == 200 and any(x.get("id") == S["diary"] for x in (r or [])),
              f"HTTP {st} 피드 {len(r or [])}건", response=r)

        call("PATCH", f"/api/diaries/{S['diary']}", S["A"], {"visibility": "PUBLIC"})

    # ── 10단계: 토큰 수명주기 ──────────────────────────────
    step("10단계: 토큰 수명주기")
    st, r = call("POST", "/api/auth/refresh", body={"refreshToken": S["A_refresh"]})
    if check("refreshToken으로 accessToken 재발급", st == 200 and r.get("accessToken"),
             f"HTTP {st} userId={r.get('userId') if isinstance(r, dict) else '?'}", response=r):
        st2, r2 = call("GET", "/api/users/me", r["accessToken"])
        check("새 accessToken으로 API 호출 정상", st2 == 200 and r2.get("id") == S["A_id"],
              f"HTTP {st2}", response=r2)
        S["A"] = r["accessToken"]

    st, r = call("GET", "/api/users/me", "eyJhbGciOiJIUzI1NiJ9.bogus.signature")
    check("잘못된 토큰 → 401 TOKEN_INVALID",
          st == 401 and isinstance(r, dict) and r.get("code") == "TOKEN_INVALID",
          f"HTTP {st} {js(r)}", response=r)

    st, r = call("GET", "/api/users/me")
    check("토큰 없음 → 401 TOKEN_MISSING",
          st == 401 and isinstance(r, dict) and r.get("code") == "TOKEN_MISSING",
          f"HTTP {st} {js(r)}", response=r)

    st, r = call("POST", "/api/auth/logout", body={"refreshToken": S["A_refresh"]})
    check("로그아웃 200", st == 200, f"HTTP {st} {js(r)}", response=r)

    st, r = call("POST", "/api/auth/refresh", body={"refreshToken": S["A_refresh"]})
    check("로그아웃한 refreshToken으로 재발급 → 401",
          st == 401, f"HTTP {st} {js(r)}", response=r)

    # ── 11단계: 탈퇴 후 동작 ──────────────────────────────
    step("11단계: 탈퇴 후 동작")
    likes_before = None
    if "diary" in S:
        st, r = call("GET", f"/api/diaries/{S['diary']}/scores", S["A"])
        likes_before = r.get("likeScore") if isinstance(r, dict) else None

    st, r = call("DELETE", "/api/users/me", S["B"])
    check("B 회원 탈퇴", st == 200, f"HTTP {st} {js(r)}", response=r)

    st, r = call("POST", "/api/auth/login", body={"email": B_EMAIL, "password": PW})
    check("탈퇴한 계정으로 로그인 → 401", st == 401, f"HTTP {st} {js(r)}", response=r)

    st, r = call("GET", f"/api/users/{S['A_id']}/followers", S["A"])
    check("A의 팔로워 목록에서 B가 빠짐",
          st == 200 and not any(u.get("userId") == S["B_id"] for u in (r or [])),
          f"HTTP {st} {js(r)}", response=r)

    if "diary" in S:
        st, r = call("GET", f"/api/diaries/{S['diary']}/likes", S["A"])
        in_list = any(u.get("userId") == S["B_id"] for u in (r or []))
        check("좋아요 목록에서 탈퇴자 B가 빠짐 (문서 명시)", st == 200 and not in_list,
              f"HTTP {st} 목록 {len(r or [])}명", response=r)

        st, r = call("GET", f"/api/diaries/{S['diary']}/scores", S["A"])
        likes_after = r.get("likeScore") if isinstance(r, dict) else None
        # 문서: likeCount는 누른 기록 그대로라 랭킹 점수가 흔들리지 않아야 한다.
        check("탈퇴해도 likeScore는 유지 (목록과 수가 다른 것이 의도된 동작)",
              likes_after == likes_before,
              f"탈퇴 전 {likes_before} → 탈퇴 후 {likes_after}", response=r)

    return S, TAG


def score_formula(relevance, color, like_score):
    """ScoreCalculator와 같은 계산. 파이썬 round()는 은행가 반올림이라 쓰지 않는다."""
    weighted = relevance * 0.5 + color * 0.3 + like_score * 0.2
    return int(weighted + 0.5)


# ────────────────────────────── 정리 ──────────────────────────────

CLEANUP_SQL = """
BEGIN;
CREATE TEMP TABLE t_users AS
  SELECT user_id FROM users WHERE email LIKE 'integ.%@example.com';
CREATE TEMP TABLE t_rooms AS
  SELECT room_id FROM drawing_rooms WHERE owner_id IN (SELECT user_id FROM t_users);
CREATE TEMP TABLE t_diaries AS
  SELECT diary_id FROM diaries WHERE room_id IN (SELECT room_id FROM t_rooms)
  UNION
  SELECT diary_id FROM diary_collaborators WHERE user_id IN (SELECT user_id FROM t_users);

DELETE FROM notifications
 WHERE receiver_id IN (SELECT user_id FROM t_users)
    OR sender_id   IN (SELECT user_id FROM t_users)
    OR (type IN ('LIKE','COMMENT') AND target_id IN (SELECT diary_id FROM t_diaries))
    OR (type = 'ROOM_INVITE'       AND target_id IN (SELECT room_id  FROM t_rooms));
DELETE FROM likes    WHERE user_id IN (SELECT user_id FROM t_users) OR diary_id IN (SELECT diary_id FROM t_diaries);
DELETE FROM comments WHERE user_id IN (SELECT user_id FROM t_users) OR diary_id IN (SELECT diary_id FROM t_diaries);
DELETE FROM ai_scores           WHERE diary_id IN (SELECT diary_id FROM t_diaries);
DELETE FROM diary_tags          WHERE diary_id IN (SELECT diary_id FROM t_diaries);
DELETE FROM diary_collaborators WHERE diary_id IN (SELECT diary_id FROM t_diaries);
DELETE FROM diaries             WHERE diary_id IN (SELECT diary_id FROM t_diaries);
DELETE FROM room_invites WHERE room_id IN (SELECT room_id FROM t_rooms)
                            OR sender_id IN (SELECT user_id FROM t_users)
                            OR receiver_id IN (SELECT user_id FROM t_users);
DELETE FROM room_members WHERE room_id IN (SELECT room_id FROM t_rooms) OR user_id IN (SELECT user_id FROM t_users);
DELETE FROM drawing_rooms WHERE room_id IN (SELECT room_id FROM t_rooms);
DELETE FROM follows    WHERE follower_id IN (SELECT user_id FROM t_users) OR following_id IN (SELECT user_id FROM t_users);
DELETE FROM categories WHERE user_id IN (SELECT user_id FROM t_users);
DELETE FROM images     WHERE uploader_id IN (SELECT user_id FROM t_users);
-- 이 실행이 만든 태그는 아무 일기도 참조하지 않을 때만 지운다(전역 사전이라 남의 것을 건드리지 않도록).
DELETE FROM tags WHERE name LIKE 'integ-%'
   AND NOT EXISTS (SELECT 1 FROM diary_tags dt WHERE dt.tag_id = tags.tag_id);
DELETE FROM users WHERE user_id IN (SELECT user_id FROM t_users);
COMMIT;
"""


def cleanup(db_url):
    """docker의 psql로 지운다. 로컬에 psql이 없어도 되도록."""
    proc = subprocess.run(
        ["docker", "run", "--rm", "-i", "-e", "PGCLIENTENCODING=UTF8",
         "postgres:18", "psql", db_url, "-v", "ON_ERROR_STOP=1"],
        input=CLEANUP_SQL.encode("utf-8"), capture_output=True)
    return proc.returncode == 0, (proc.stdout + proc.stderr).decode("utf-8", "replace")


# ────────────────────────────── 리포트 ──────────────────────────────

def write_report(path, base, started, passed, failed, cleaned, cleanup_log, session, tag):
    L = []
    L.append("# 통합 테스트 리포트\n")
    L.append(f"- **실행 시각**: {started.strftime('%Y-%m-%d %H:%M:%S')} (KST 기준 로컬 시각)")
    L.append(f"- **대상 서버**: `{base}`")
    L.append(f"- **결과**: {passed} PASS / {failed} FAIL (총 {passed + failed})")
    L.append(f"- **데이터 정리**: {cleaned}\n")

    L.append("## 단계별 결과\n")
    cur = None
    for stp, label, ok, detail in RESULTS:
        if stp != cur:
            cur = stp
            n_f = sum(1 for s, _, o, _ in RESULTS if s == stp and not o)
            L.append(f"\n### {stp} — {'✅ 전부 통과' if n_f == 0 else f'❌ {n_f}건 실패'}\n")
        L.append(f"- {'✅' if ok else '❌'} {label}" + (f" — `{detail}`" if detail else ""))

    if NOTES:
        L.append("\n## 관찰 — 실패는 아니지만 알아둘 것\n")
        for stp, text in NOTES:
            L.append(f"- **[{stp.split(':')[0]}]** {text}")

    if FAILURES:
        L.append("\n## 실패 상세 (요청/응답 전문)\n")
        for i, f in enumerate(FAILURES, 1):
            L.append(f"\n### {i}. [{f['step']}] {f['label']}\n")
            L.append(f"- 판정 근거: `{f['detail']}`")
            if f.get("request") is not None:
                L.append(f"- 요청 본문:\n```json\n{json.dumps(f['request'], ensure_ascii=False, indent=2)}\n```")
            L.append(f"- 응답:\n```json\n{json.dumps(f['response'], ensure_ascii=False, indent=2, default=str)}\n```")

    L.append("\n## 이 실행이 만든 데이터\n")
    L.append("> `integ.` 로 시작하는 계정과 거기 딸린 모든 데이터는 **이 스크립트가 만든 테스트용**이다.")
    L.append("> 스크립트가 중간에 죽어 정리가 안 된 경우에도 같은 기준으로 지우면 된다.\n")
    L.append(f"- 계정: {CREATED['users'] or '없음'}")
    L.append(f"- diaryId: {CREATED['diaries'] or '없음'}")
    L.append(f"- roomId: {CREATED['rooms'] or '없음'}")
    L.append(f"- imageId: {CREATED['images'] or '없음'}")
    L.append(f"- categoryId: {CREATED['categories'] or '없음'}")
    L.append(f"- commentId: {CREATED['comments'] or '없음'}")
    L.append(f"- 태그 이름: {CREATED['tags'] or '없음'}")

    if cleanup_log:
        L.append(f"\n### 정리 로그\n```\n{cleanup_log.strip()[:2000]}\n```")

    L.append("\n## 수동 정리 방법\n")
    L.append("정리를 건너뛴 경우 아래를 실행하면 `integ.` 계정과 딸린 데이터가 전부 지워진다.\n")
    L.append("```bash\npsql \"$RAILWAY_DB_URL\" -f infra/integration-cleanup.sql\n```")

    io.open(path, "w", encoding="utf-8", newline="\n").write("\n".join(L) + "\n")


# ────────────────────────────── 진입점 ──────────────────────────────

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default=DEFAULT_BASE)
    ap.add_argument("--db-url", default=os.environ.get("RAILWAY_DB_URL"))
    ap.add_argument("--report", default=os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                                     "integration-test-report.md"))
    ap.add_argument("--no-cleanup", action="store_true", help="전부 통과해도 데이터를 남긴다")
    args = ap.parse_args()

    BASE = args.base.rstrip("/")
    started = datetime.datetime.now()
    print(f"대상: {BASE}\n시작: {started:%Y-%m-%d %H:%M:%S}")

    session, tag = run()

    passed = sum(1 for *_, ok, _ in [(a, b, c, d) for a, b, c, d in RESULTS] if ok)
    failed = len(RESULTS) - passed

    print(f"\n{'═' * 74}\n결과: {passed} PASS / {failed} FAIL")
    if failed:
        print("\n실패 목록:")
        for f in FAILURES:
            print(f"  ✗ [{f['step']}] {f['label']} — {f['detail'][:120]}")

    cleaned, log = "건너뜀", ""
    if failed:
        cleaned = "**건너뜀** — 실패한 단계가 있어 원인 분석을 위해 데이터를 남겼다"
    elif args.no_cleanup:
        cleaned = "건너뜀 (--no-cleanup)"
    elif not args.db_url:
        cleaned = "**건너뜀** — DB 연결 문자열이 없다(--db-url 또는 RAILWAY_DB_URL)"
    else:
        ok, log = cleanup(args.db_url)
        cleaned = "완료 (DB에서 실제 DELETE)" if ok else f"**실패** — {log[:200]}"
    print(f"정리: {cleaned}")

    write_report(args.report, BASE, started, passed, failed, cleaned, log, session, tag)
    print(f"리포트: {args.report}")
    sys.exit(1 if failed else 0)
