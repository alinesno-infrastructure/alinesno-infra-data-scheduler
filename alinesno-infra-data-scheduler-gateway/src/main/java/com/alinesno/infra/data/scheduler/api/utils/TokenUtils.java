package com.alinesno.infra.data.scheduler.api.utils;

public class TokenUtils {

    public static String getToken(String token) {
        return "Bearer " + token ;
    }

}
