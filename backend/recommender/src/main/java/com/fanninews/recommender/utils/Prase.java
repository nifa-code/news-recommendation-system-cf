package com.fanninews.recommender.utils;

public class Prase {
    private Long parseNewsId(Object newsIdObj) {
        if (newsIdObj instanceof String) {
            return Long.parseLong((String) newsIdObj);
        } else if (newsIdObj instanceof Integer) {
            return ((Integer) newsIdObj).longValue();
        } else if (newsIdObj instanceof Long) {
            return (Long) newsIdObj;
        } else {
            throw new IllegalArgumentException("无法解析news_id类型: " + newsIdObj.getClass());
        }
    }
    private Double getDoubleValue(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        } else if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
    private String getStringValue(Object obj) {
        return obj != null ? obj.toString() : "";
    }
    private String extractSummary(String content) {
        if (content == null || content.isEmpty()) return "";
        // 简单取前100字符
        String plainText = content.replaceAll("<[^>]+>", "");
        return plainText.length() > 100 ? plainText.substring(0, 100) + "..." : plainText;
    }
}
