package uw.mfa.captcha.vo;

import java.util.Objects;

/**
 * 坐标信息 (滑块和点选文字方式都会使用)
 */
public class CaptchaPoint {

    public int x;

    public int y;

    public CaptchaPoint() {
    }

    public CaptchaPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String toJsonString() {
        return String.format( "{\"x\":%d,\"y\":%d}", x, y );
    }

    @Override
    public int hashCode() {
        return Objects.hash( x, y );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CaptchaPoint pointVO = (CaptchaPoint) o;
        return x == pointVO.x && y == pointVO.y;
    }
}
