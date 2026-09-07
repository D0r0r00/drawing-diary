package com.drawingdiary.backend.domain.image;

import org.springframework.http.MediaType;

/**
 * 바이트 앞머리(매직 넘버)로 이미지 형식을 판별한다.
 *
 * 업로드 API는 클라이언트가 보낸 Content-Type을 믿어도 되지만, AI 서버는 헤더에
 * image/png를 붙여놓고 실제로는 JPEG를 돌려준다. 선언값을 그대로 저장하면 우리
 * /api/images/{id}가 nosniff와 함께 잘못된 Content-Type으로 서빙하게 되고, 브라우저가
 * 형식 추측을 못 해 그림이 깨진다. 그래서 외부에서 받아온 바이트는 여기서 다시 확인한다.
 */
public final class ImageFormats {

    public static final String IMAGE_WEBP_VALUE = "image/webp";

    private ImageFormats() {
    }

    /**
     * @return 판별된 Content-Type, 알 수 없으면 null
     */
    public static String detect(byte[] data) {
        if (data == null) {
            return null;
        }
        if (startsWith(data, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (startsWith(data, 0xFF, 0xD8, 0xFF)) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        // WebP는 RIFF 컨테이너라 4~7바이트(길이)를 건너뛰고 8바이트째부터 "WEBP"를 본다.
        if (startsWith(data, 'R', 'I', 'F', 'F') && data.length >= 12
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') {
            return IMAGE_WEBP_VALUE;
        }
        return null;
    }

    private static boolean startsWith(byte[] data, int... expected) {
        if (data.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((data[i] & 0xFF) != (expected[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }
}
