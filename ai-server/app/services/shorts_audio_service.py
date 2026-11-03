import math
import shutil
import subprocess
import tempfile
import wave
import hashlib

from pathlib import Path

import numpy as np


# ============================
# 기본 설정
# ============================

SAMPLE_RATE = 22050
DURATION = 15

TOTAL_SAMPLES = SAMPLE_RATE * DURATION


# ============================
# 분위기 분석
# ============================

def select_mood(analysis):

    mood = (
        str(analysis.get("mood", "")) + " " +
        str(analysis.get("music", ""))
    ).lower()

    if any(word in mood for word in [
        "슬픈", "우울", "쓸쓸", "sad", "melancholy"
    ]):
        return "sad"

    if any(word in mood for word in [
        "차분", "평화", "잔잔", "고요", "calm"
    ]):
        return "calm"

    return "happy"


# ============================
# 음계 변환
# ============================

def note_frequency(midi_note):

    return 440.0 * (
        2 ** ((midi_note - 69) / 12)
    )


# ============================
# 악기 소리 생성
# ============================

def make_note(frequency, duration, volume=0.15):

    length = max(
        1,
        int(SAMPLE_RATE * duration)
    )

    t = np.arange(length) / SAMPLE_RATE

    # 기본음 + 배음
    sound = (
        np.sin(2 * np.pi * frequency * t)
        + 0.35 * np.sin(2 * np.pi * frequency * 2 * t)
        + 0.12 * np.sin(2 * np.pi * frequency * 3 * t)
    )

    # 피아노풍 감쇠 효과
    envelope = np.exp(-3.5 * t / max(duration, 0.01))

    # 시작 부분 클릭음 방지
    attack = min(
        int(SAMPLE_RATE * 0.015),
        length
    )

    if attack > 0:
        envelope[:attack] *= np.linspace(
            0, 1, attack
        )

    return sound * envelope * volume


# ============================
# 오디오 합성 보조 함수
# ============================

def mix_sound(target, sound, start_time):

    start = int(start_time * SAMPLE_RATE)

    if start >= len(target):
        return

    if start < 0:
        sound = sound[-start:]
        start = 0

    end = min(
        start + len(sound),
        len(target)
    )

    if end > start:
        target[start:end] += sound[:end - start]


# ============================
# 배경음악 생성
# ============================

def create_background_music(mood):

    audio = np.zeros(
        TOTAL_SAMPLES,
        dtype=np.float64
    )

    # 분위기에 맞는 음계와 템포
    music_settings = {
        "happy": {
            "bpm": 115,
            "notes": [60, 64, 67, 72, 67, 64, 62, 67],
            "chords": [
                [48, 52, 55],
                [53, 57, 60],
                [55, 59, 62],
                [48, 52, 55]
            ]
        },

        "calm": {
            "bpm": 75,
            "notes": [60, 62, 64, 67, 64, 62, 60, 55],
            "chords": [
                [48, 52, 55],
                [45, 48, 52],
                [53, 57, 60],
                [48, 52, 55]
            ]
        },

        "sad": {
            "bpm": 65,
            "notes": [57, 60, 64, 60, 59, 57, 55, 52],
            "chords": [
                [45, 48, 52],
                [41, 45, 48],
                [43, 47, 50],
                [45, 48, 52]
            ]
        }
    }

    settings = music_settings[mood]

    bpm = settings["bpm"]

    beat_duration = 60 / bpm

    notes = settings["notes"]
    chords = settings["chords"]

    # 1. 메인 멜로디 생성
    current_time = 0
    index = 0

    while current_time < DURATION:

        midi_note = notes[index % len(notes)]

        frequency = note_frequency(midi_note)

        note = make_note(
            frequency,
            beat_duration * 0.9,
            volume=0.10
        )

        mix_sound(
            audio,
            note,
            current_time
        )

        current_time += beat_duration
        index += 1

    # 2. 화음 생성
    current_time = 0
    chord_index = 0

    chord_duration = beat_duration * 4

    while current_time < DURATION:

        chord = chords[
            chord_index % len(chords)
        ]

        for midi_note in chord:

            frequency = note_frequency(midi_note)

            note = make_note(
                frequency,
                chord_duration,
                volume=0.025
            )

            mix_sound(
                audio,
                note,
                current_time
            )

        current_time += chord_duration
        chord_index += 1

    # 3. 전체 페이드인/아웃
    fade_length = SAMPLE_RATE

    audio[:fade_length] *= np.linspace(
        0, 1, fade_length
    )

    audio[-fade_length:] *= np.linspace(
        1, 0, fade_length
    )

    return audio


# ============================
# 새소리 생성
# ============================

def make_bird_sound():

    duration = 0.8

    length = int(SAMPLE_RATE * duration)

    t = np.arange(length) / SAMPLE_RATE

    # 시간에 따라 높아졌다 낮아지는 주파수
    frequency = (
        1500
        + 500 * np.sin(2 * np.pi * 6 * t)
        + 300 * t
    )

    phase = 2 * np.pi * np.cumsum(
        frequency
    ) / SAMPLE_RATE

    sound = np.sin(phase)

    # 짧게 끊어지는 지저귐
    chirp_envelope = (
        np.maximum(
            0,
            np.sin(2 * np.pi * 4 * t)
        ) ** 2
    )

    envelope = (
        np.sin(np.pi * t / duration) ** 2
    ) * chirp_envelope

    return sound * envelope * 0.16


# ============================
# 바람 소리 생성
# ============================

def make_wind_sound(rng):

    duration = 2.0

    length = int(SAMPLE_RATE * duration)

    noise = rng.normal(
        0, 1, length
    )

    # 노이즈를 부드럽게 처리
    kernel = np.ones(300) / 300

    smooth_noise = np.convolve(
        noise,
        kernel,
        mode="same"
    )

    peak = np.max(np.abs(smooth_noise))

    if peak > 0:
        smooth_noise /= peak

    t = np.arange(length) / SAMPLE_RATE

    # 바람 세기 변화
    envelope = (
        np.sin(np.pi * t / duration) ** 2
    )

    return smooth_noise * envelope * 0.12


# ============================
# 빗소리 생성
# ============================

def make_rain_sound(rng):

    duration = 2.0

    length = int(SAMPLE_RATE * duration)

    audio = rng.normal(
        0, 0.018, length
    )

    # 불규칙한 빗방울
    for _ in range(80):

        position = int(
            rng.integers(0, length)
        )

        drop_length = min(
            120,
            length - position
        )

        if drop_length <= 0:
            continue

        drop = rng.normal(
            0, 1, drop_length
        )

        envelope = np.exp(
            -np.linspace(0, 5, drop_length)
        )

        audio[position:position + drop_length] += (
            drop * envelope * 0.08
        )

    # 자연스러운 시작과 끝
    fade = min(int(SAMPLE_RATE * 0.1), length // 2)

    audio[:fade] *= np.linspace(0, 1, fade)
    audio[-fade:] *= np.linspace(1, 0, fade)

    return audio


# ============================
# 강아지 짖는 소리
# ============================

def make_dog_sound():

    duration = 0.6

    length = int(SAMPLE_RATE * duration)

    t = np.arange(length) / SAMPLE_RATE

    # 주파수가 빠르게 떨어지는 소리
    frequency = (
        350 + 170 * np.exp(-12 * t)
    )

    phase = 2 * np.pi * np.cumsum(
        frequency
    ) / SAMPLE_RATE

    sound = (
        np.sin(phase)
        + 0.4 * np.sin(2 * phase)
        + 0.2 * np.sin(3 * phase)
    )

    envelope = np.exp(-7 * t)

    attack = min(int(SAMPLE_RATE * 0.01), length)

    envelope[:attack] *= np.linspace(
        0, 1, attack
    )

    return sound * envelope * 0.17


# ============================
# 차임벨 소리
# ============================

def make_chime_sound():

    duration = 1.5

    length = int(SAMPLE_RATE * duration)

    t = np.arange(length) / SAMPLE_RATE

    sound = (
        np.sin(2 * np.pi * 880 * t)
        + 0.5 * np.sin(2 * np.pi * 1320 * t)
        + 0.25 * np.sin(2 * np.pi * 1760 * t)
    )

    envelope = np.exp(-3.5 * t)

    return sound * envelope * 0.10


# ============================
# 웃음 느낌의 효과음
# ============================

def make_laugh_sound():

    audio = np.zeros(
        int(SAMPLE_RATE * 0.8)
    )

    for index in range(3):

        tone = make_note(
            650 + index * 100,
            0.12,
            volume=0.12
        )

        mix_sound(
            audio,
            tone,
            index * 0.2
        )

    return audio


# ============================
# 효과음 선택
# ============================

def create_effect(description, rng):

    description = str(description).lower()

    if any(word in description for word in [
        "새", "지저귀", "bird"
    ]):
        return make_bird_sound()

    if any(word in description for word in [
        "강아지", "멍멍", "짖", "dog"
    ]):
        return make_dog_sound()

    if any(word in description for word in [
        "바람", "wind"
    ]):
        return make_wind_sound(rng)

    if any(word in description for word in [
        "비", "빗소리", "rain"
    ]):
        return make_rain_sound(rng)

    if any(word in description for word in [
        "웃음", "laugh"
    ]):
        return make_laugh_sound()

    if any(word in description for word in [
        "차임", "벨", "종소리", "chime"
    ]):
        return make_chime_sound()

    # 매칭되는 효과음이 없으면 생략
    return None


# ============================
# 최종 오디오 생성
# ============================

def create_audio(analysis):

    mood = select_mood(analysis)

    print("선택된 음악 분위기:", mood)

    # 동일 분석 결과에 동일한 노이즈 생성
    seed_text = str(analysis)

    seed = int.from_bytes(
        hashlib.sha256(
            seed_text.encode("utf-8")
        ).digest()[:8],
        "big"
    )

    rng = np.random.default_rng(seed)

    audio = create_background_music(mood)

    scenes = analysis.get("scenes", [])

    current_time = 0

    for scene in scenes:

        sound_description = scene.get(
            "sound", ""
        )

        effect = create_effect(
            sound_description,
            rng
        )

        if effect is not None:

            print(
                "효과음 생성:",
                sound_description
            )

            mix_sound(
                audio,
                effect,
                current_time + 0.5
            )

        current_time += float(
            scene.get("duration", 0)
        )

    # 과도한 음량 방지
    peak = np.max(np.abs(audio))

    if peak > 0.95:
        audio = audio / peak * 0.95

    return audio


# ============================
# WAV 저장
# ============================

def save_wav(audio, wav_path):

    audio = np.clip(
        audio,
        -1.0,
        1.0
    )

    pcm = (
        audio * 32767
    ).astype(np.int16)

    with wave.open(
        str(wav_path),
        "wb"
    ) as wav_file:

        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(SAMPLE_RATE)

        wav_file.writeframes(
            pcm.tobytes()
        )


# ============================
# FFmpeg 영상 합성
# ============================

def add_audio_to_video(video_path, analysis):

    video_path = Path(video_path)

    final_path = video_path.with_name(
        video_path.stem + "_audio.mp4"
    )

    # 오디오 생성
    audio = create_audio(analysis)

    ffmpeg_path = shutil.which("ffmpeg")

    if not ffmpeg_path:
        raise RuntimeError(
            "FFmpeg를 찾을 수 없습니다. PATH를 확인하세요."
        )

    with tempfile.TemporaryDirectory() as temp_dir:

        wav_path = Path(temp_dir) / "audio.wav"

        save_wav(
            audio,
            wav_path
        )

        command = [
            ffmpeg_path,
            "-y",

            "-i", str(video_path),
            "-i", str(wav_path),

            "-map", "0:v:0",
            "-map", "1:a:0",

            "-c:v", "libx264",
            "-preset", "fast",
            "-pix_fmt", "yuv420p",

            "-c:a", "aac",
            "-b:a", "128k",

            "-t", str(DURATION),

            "-movflags", "+faststart",

            str(final_path)
        ]

        try:
            subprocess.run(
                command,
                check=True,
                capture_output=True,
                text=True
            )

        except subprocess.CalledProcessError as e:
            print("FFmpeg 오류:")
            print(e.stderr[-3000:])
            raise

    print("오디오 포함 영상 생성 완료")
    print("저장 위치:", final_path)

    return final_path