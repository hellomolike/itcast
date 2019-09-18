package com.leyou.auth.test;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.ustils.JwtUtils;
import com.leyou.auth.ustils.RsaUtils;
import org.junit.Before;
import org.junit.Test;

import java.security.PrivateKey;
import java.security.PublicKey;

public class JwtTest {

    private static final String pubKeyPath = "C:\\tmp\\rsa\\rsa.pub";

    private static final String priKeyPath = "C:\\tmp\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @Before
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        // 生成token
        String token = JwtUtils.generateToken(new UserInfo(20L, "jack"), privateKey, 5);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6MjAsInVzZXJuYW2lIjoiamFjayIsImV4cCI6MTUzNTE2OTMxOX0.BmtvgJlWKGC10_smm6xQlkgI3ypiXNW67cMH65lsOql5S1-46b6BSNn-fscnElLOiD4zIa9pcNqWgvfez0-CsNFBvllH9a3Ok0uSPH48O79YPAfOrq8I69S_gocwtfBuzM4Q2rtJ0Az0APuIXqS0qoOSWlEC-LCUcujsKaszgg4";

        // 解析token
        UserInfo user = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + user.getId());
        System.out.println("userName: " + user.getUsername());
    }
}