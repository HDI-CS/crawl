package kr.co.hdi.user.exception;


import kr.co.hdi.global.exception.CustomException;

public class AuthException extends CustomException {
    public AuthException(AuthErrorCode errorCode) {
        super(errorCode);
    }

    public AuthException(AuthErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
