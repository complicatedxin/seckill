package com.sauvignon.seckill.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.sauvignon.seckill.constants.Constants;

import java.util.Calendar;
import java.util.Map;

public class JwtUtil
{
    private static Algorithm encryptedSign = Algorithm.HMAC256(Constants.signature);

    /**
     * 生成token
     *
     * @param map //传入payload
     * @return 返回token
     */
    public static String generateToken(Map<String, Object> map, int calendarTimeUnit, int time)
    {
        JWTCreator.Builder builder = JWT.create();
        builder.withPayload(map);
        Calendar instance = Calendar.getInstance();
        instance.add(calendarTimeUnit, time);
        builder.withExpiresAt(instance.getTime());
        return builder.sign(encryptedSign);
    }

    /**
     * 验证token
     *
     * @param token
     * @return
     */
    public static void verify(String token)
            throws AlgorithmMismatchException, SignatureVerificationException,
                    TokenExpiredException, InvalidClaimException
    {
        JWT.require(encryptedSign).build().verify(token);  // 如果验证通过，则不会报错
    }

    /**
     * 获取获取token中payload
     *
     * @param token
     * @return
     */
    public static Map<String, Claim> getPayload(String token)
            throws AlgorithmMismatchException, SignatureVerificationException,
                    TokenExpiredException, InvalidClaimException
    {
        return JWT.require(encryptedSign).build().verify(token).getClaims();
    }




    public static Long getUserId(String token)
            throws AlgorithmMismatchException, SignatureVerificationException,
            TokenExpiredException, InvalidClaimException
    {
        return JWT.require(encryptedSign).build().verify(token).getClaims()
                .get("userId").asLong();
    }
}
