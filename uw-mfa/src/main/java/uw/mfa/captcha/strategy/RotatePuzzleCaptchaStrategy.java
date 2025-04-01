package uw.mfa.captcha.strategy;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.dto.ResponseData;
import uw.common.util.JsonUtils;
import uw.mfa.captcha.CaptchaStrategy;
import uw.mfa.captcha.util.CaptchaImageUtils;
import uw.mfa.captcha.util.CaptchaRandomUtils;
import uw.mfa.captcha.vo.CaptchaData;
import uw.mfa.captcha.vo.CaptchaPoint;
import uw.mfa.captcha.vo.CaptchaQuestion;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * 描述: 旋转
 */
public class RotatePuzzleCaptchaStrategy implements CaptchaStrategy {
    private static final Logger logger = LoggerFactory.getLogger( RotatePuzzleCaptchaStrategy.class );

    /**
     * 误差
     */
    protected static int ROTATE_OFFSET = 10;

    /**
     * 获取验证码
     *
     * @param captchaId 行为验证码id
     * @return
     */
    @Override
    public ResponseData<CaptchaData> generate(String captchaId) {
        // 旋转底图 抠出圆形
        BufferedImage rotateMainImage = CaptchaImageUtils.getRotateMainImage();
        if (null == rotateMainImage) {
            return ResponseData.errorMsg( "RotatePuzzle main image load failed!" );
        }
        // 切割圆形
        try {
            CaptchaQuestion captchaQuestion = new CaptchaQuestion();

            int rotateWidth = rotateMainImage.getWidth();
            int rotateHeight = rotateMainImage.getHeight();

            // 随机生成旋转拼图坐标 需要生成旋转图的偏移量、旋转角度
            int radius = 50; // 圆的半径
            int x = CaptchaRandomUtils.getRandomInt(rotateWidth - 2 * radius) + radius; // 圆心的x坐标
            int y = CaptchaRandomUtils.getRandomInt( rotateHeight - 2 * radius ) + radius; // 圆心的y坐标

            // 此方法只能切割正方形
            BufferedImage formatAvatarImage = rotateMainImage.getSubimage( x - radius, y - radius, radius * 2, radius * 2 );
            // 将正方形转换为圆形
            BufferedImage circleImage = changeCircleImage( formatAvatarImage );

            // 模糊被且区域
            CaptchaImageUtils.addLacuna( rotateMainImage, circleImage, x - radius, y - radius );

            // 旋转角度
            int degree = CaptchaRandomUtils.getRandomInt( 60,300 );
            // 旋转后的新图片 旋转是逆时针旋转
            BufferedImage degreeImage = CaptchaImageUtils.rotateImage( circleImage, degree );
            // 移动距离答案 页面可旋转一圈 即360度 用360减去偏移量(前端旋转是逆时针旋转) 即为需要移动的距离 直接将拖动条设置为360
            int answer = 360 - degree;

            captchaQuestion.setMainImageBase64( CaptchaImageUtils.imageToBase64( rotateMainImage ) );
            captchaQuestion.setSubImageBase64( CaptchaImageUtils.imageToBase64( degreeImage ) );
            captchaQuestion.setCaptchaId( captchaId );
            captchaQuestion.setCaptchaType( captchaType() );
            captchaQuestion.setSubData( JsonUtils.toString(new CaptchaPoint( x, y ) ));
            String captchaResult = String.valueOf( answer );
            return  ResponseData.success(new CaptchaData( captchaQuestion, captchaResult )) ;
        } catch (Exception e) {
            return ResponseData.errorMsg( "RotatePuzzle captcha generate failed! "+e.getMessage() );
        }
    }

    /**
     * 校验验证码
     *
     * @param answerData
     * @param captchaResult
     * @return
     */
    @Override
    public ResponseData verify(String answerData, String captchaResult) {
        // 实际记录答案
        int rotatePuzzleAnswer = Integer.parseInt(captchaResult);
        double parsed = Double.parseDouble(answerData);
        int move = (int) parsed;
        // 如果角度大于误差
        if (Math.abs(move - rotatePuzzleAnswer) > ROTATE_OFFSET) {
            return ResponseData.error();
        }
        //校验成功
        return ResponseData.success();
    }

    /**
     * 获得人类正常操作毫秒数。
     * 检测是否是人类，当前只有一个操作时间。
     *
     * @return
     */
    @Override
    public long humanOpTime() {
        return 2000;
    }


    /**
     * 将正方形转换为圆形
     *
     * @param formatAvatarImage
     * @return
     */
    private static BufferedImage changeCircleImage(BufferedImage formatAvatarImage) {
        // 从正方形绘制圆形出来
        // 设置圆形剪切区域的中心和半径
        int x = formatAvatarImage.getWidth() / 2;
        int y = formatAvatarImage.getHeight() / 2;
        int radius = Math.min( x, y );

        // 创建一个新的简单的 image, 使用 RGB 颜色模型, 默认的 sRGB 色彩空间, 给定的 BufferedImage 类型
        BufferedImage circleImage = new BufferedImage( radius * 2, radius * 2, BufferedImage.TYPE_INT_ARGB );

        // 创建 Graphics2D, 用于绘制新的图片
        Graphics2D g2d = circleImage.createGraphics();
        // 绘制圆形剪切区域
        Ellipse2D.Double ellipse = new Ellipse2D.Double( 0, 0, radius * 2, radius * 2 );
        g2d.setClip( ellipse );
        // 绘制原始图片到新的图片上
        g2d.drawImage( formatAvatarImage, x - radius, y - radius, null );
        // 释放 Graphics2D 资源
        g2d.dispose();
        return circleImage;
    }



}
