package uw.mfa.captcha.vo;

import java.util.Objects;

/**
 * Captcha坐标信息（滑块拼图与点选文字方式共用）。
 */
public class CaptchaPoint {

    /**
     * x坐标。
     */
    public int x;

    /**
     * y坐标。
     */
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

    /**
     * 序列化为精简JSON字符串（无空格），如 {@code {"x":85,"y":34}}。
     *
     * @return JSON字符串
     */
    public String toJsonString() {
        return String.format("{\"x\":%d,\"y\":%d}", x, y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
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
