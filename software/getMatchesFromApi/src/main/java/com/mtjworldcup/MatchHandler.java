package com.mtjworldcup;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class MatchHandler implements RequestHandler<Void, String> {
    @Override
    public String handleRequest(Void input, Context context) {
        return "hello world";
    }
}
