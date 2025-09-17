package com.example.gps;

public class MarkerInfo {
    private String name;
    private String description;
    private String imageUrl;
    private String type;

    public MarkerInfo(String name, String description, String imageUrl, String type) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getType() {
        return type;
    }

    public static MarkerInfo getMarkerInfo(String name) {
        switch (name) {
            case "산성로터리":
                return new MarkerInfo(
                    "산성로터리",
                    "남한산성 둘레길의 시작점이자 중심지입니다. 주차장과 관광안내소가 있으며, 둘레길 코스 선택의 기준점이 됩니다.",
                    "photo_sansungrotary.png",
                    "시작점"
                );
            case "서문":
                return new MarkerInfo(
                    "서문",
                    "남한산성의 서쪽 성문으로, 조선시대에 지어진 역사적인 건축물입니다. 성벽과 함께 조선시대의 방어 체계를 엿볼 수 있습니다.",
                    "photo_seomun.png",
                    "성문"
                );
            case "북문":
                return new MarkerInfo(
                    "북문",
                    "남한산성의 북쪽 성문으로, 서울 방향으로 향하는 주요 관문이었습니다. 성벽의 웅장한 모습을 감상할 수 있습니다.",
                    "photo_bukmun.png",
                    "성문"
                );
            case "남문":
                return new MarkerInfo(
                    "남문",
                    "남한산성의 남쪽 성문으로, 성남시 방향으로 향하는 관문입니다. 주변의 자연 경관이 아름답습니다.",
                    "photo_nammun.png",
                    "성문"
                );
            case "동문":
                return new MarkerInfo(
                    "동문",
                    "남한산성의 동쪽 성문으로, 광주 방향으로 향하는 관문입니다. 성벽과 함께 아름다운 산세를 감상할 수 있습니다.",
                    "photo_dongmun.png",
                    "성문"
                );
            case "천주사터":
                return new MarkerInfo(
                    "천주사터",
                    "조선시대에 지어진 사찰의 터로, 현재는 그 흔적만 남아있습니다. 역사적 의미가 깊은 장소입니다.",
                    "photo_cheonjusateo.png",
                    "사찰터"
                );
            case "현절사":
                return new MarkerInfo(
                    "현절사",
                    "남한산성 내에 위치한 사찰로, 조선시대의 건축 양식을 잘 보존하고 있습니다.",
                    "photo_hyeonjeolsa.png",
                    "사찰"
                );
            case "장경사":
                return new MarkerInfo(
                    "장경사",
                    "남한산성의 대표적인 사찰 중 하나로, 아름다운 자연 속에 자리잡고 있습니다.",
                    "photo_janggyeongsa.png",
                    "사찰"
                );
            case "망월사":
                return new MarkerInfo(
                    "망월사",
                    "남한산성의 유명한 사찰로, 조선시대의 건축물이 잘 보존되어 있습니다.",
                    "photo_mangwolsa.png",
                    "사찰"
                );
            case "영월정":
                return new MarkerInfo(
                    "영월정",
                    "조선시대에 지어진 정자로, 주변의 아름다운 경관을 감상할 수 있는 곳입니다.",
                    "photo_yeongwoljeong.png",
                    "정자"
                );
            case "수어장대":
                return new MarkerInfo(
                    "수어장대",
                    "조선시대의 군사 시설로, 성을 지키는 중요한 역할을 했던 곳입니다.",
                    "photo_sueojangdae.png",
                    "군사시설"
                );
            case "남한산성세계유산센터":
                return new MarkerInfo(
                    "남한산성세계유산센터",
                    "남한산성의 역사와 문화를 소개하는 전시관으로, 다양한 문화재와 유물을 관람할 수 있습니다.",
                    "photo_heritage_center.png",
                    "전시관"
                );
            case "국청사":
                return new MarkerInfo(
                    "국청사",
                    "남한산성의 대표적인 사찰 중 하나로, 조선시대의 건축 양식을 잘 보존하고 있습니다.",
                    "photo_gukcheonsa.png",
                    "사찰"
                );
            case "숭렬전":
                return new MarkerInfo(
                    "숭렬전",
                    "조선시대의 건축물로, 역사적 의미가 깊은 장소입니다.",
                    "photo_sungryeoljeon.png",
                    "건축물"
                );
            case "벌봉":
                return new MarkerInfo(
                    "벌봉",
                    "남한산성의 최고봉으로, 탁 트인 전망을 감상할 수 있는 곳입니다.",
                    "photo_beolbong.png",
                    "정상"
                );
            default:
                return new MarkerInfo(
                    name,
                    "정보가 준비 중입니다.",
                    "",
                    "기타"
                );
        }
    }
} 