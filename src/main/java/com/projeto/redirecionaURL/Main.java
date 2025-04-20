package com.projeto.redirecionaURL;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
    public class Main implements RequestHandler<Map<String, Object>, Map<String, Object>> {

        private final S3Client s3Client = S3Client.builder().build();

        private final ObjectMapper objectMapper = new ObjectMapper();
        @Override
        public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {

            String pathParameter = (String) input.get("originalPath");
            String shortUrlCode = pathParameter.replace("/", "");

            if(shortUrlCode == null || shortUrlCode.isEmpty()) {
                throw new IllegalArgumentException("Invalid input: is required 'shortUrlCode'");
            }

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket("umc-acess-url")
                    .key(shortUrlCode + ".json" )
                    .build();

            InputStream s3ObjectStream;

            try{
                s3ObjectStream = s3Client.getObject(getObjectRequest);

            } catch( Exception exception) {
                throw new RuntimeException("Error getting Url from data s3" + exception.getMessage(), exception);

            }
            System.out.println("chegou na declaração do objeto urldata");

            UrlData urlData; // criado um objeto para salvar as informações do JSON

            try {
                urlData = objectMapper.readValue(s3ObjectStream, UrlData.class); // faz a leitura do json s3object e
                // transforma em um classe java "UrlData.class"

            } catch (Exception e) {
                throw new RuntimeException("Error to salve the information into object" + e.getMessage(), e);

            }
            long currentTimeInSeconds = System.currentTimeMillis() / 1000;
            Map<String, Object> response = new HashMap<>();
            System.out.println("chegou no response map");
            //url has expired
            if( urlData.getExpirationTime() < currentTimeInSeconds) {
                response.put("statusCode", 410);
                response.put("body", "This URL has expired");
                return response;
            }
            // url still in time to use
            response.put("statusCode", 302);
            Map<String, String> headers = new HashMap<>();
            headers.put("location", urlData.getOriginalUrl());
            response.put("headers", headers);

            return response;


        }
}
