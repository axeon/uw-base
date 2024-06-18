package uw.mfa.captcha.strategy;


import org.apache.commons.lang3.StringUtils;
import uw.common.dto.ResponseData;
import uw.mfa.captcha.CaptchaStrategy;
import uw.mfa.captcha.util.CaptchaImageUtils;
import uw.mfa.captcha.vo.CaptchaData;
import uw.mfa.captcha.vo.CaptchaQuestion;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 描述: 计算式验证码生成策略
 */
public class CalculateCaptchaStrategy implements CaptchaStrategy {

    private static final char[] OP = new char[]{'+', '-'};

    /**
     * 图片背景色
     */
    private static final Color BG_COLOR = new Color( 253, 251, 255 );

    /**
     * 随机生成器
     */
    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    /**
     * 图片宽度
     */
    private static final int WIDTH = 360;

    /**
     * 图片高度
     */
    private static final int HEIGHT = 140;

    /**
     * 字体大小
     */
    private static final int FONT_SIZE = 28;

    /**
     * 图片行数
     */
    private static final int LINE_NUM = 1;

    /**
     * 字体样式。
     */
    private static final Font[] FONTS = new Font[]{new Font( "Times New Roman", Font.BOLD, FONT_SIZE ), new Font( "Verdana", Font.BOLD, FONT_SIZE ), new Font( "Ravie", Font.BOLD
            , FONT_SIZE ), new Font( "arial", Font.BOLD, FONT_SIZE ), new Font( "Antique Olive Compact", Font.BOLD, FONT_SIZE ), new Font( "Fixedsys", Font.BOLD, FONT_SIZE ),
            new Font( "Wide Latin", Font.BOLD, FONT_SIZE ), new Font( "Gill " + "Sans Ultra Bold", Font.BOLD, FONT_SIZE )};

    /**
     * 获取验证码
     *
     * @param captchaId 行为验证码id
     * @return
     */
    @Override
    public ResponseData<CaptchaData> generate(String captchaId) {
        CaptchaQuestion captchaQuestion = new CaptchaQuestion();
        //初始化画布
        BufferedImage image = new BufferedImage( WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB );
        Graphics graphics = image.getGraphics();
        graphics.setColor( BG_COLOR );
        graphics.fillRect( 0, 0, WIDTH, HEIGHT );
        mixSource( image );
        String captchaResult = drawCode( graphics );

        captchaQuestion.setMainImageBase64( CaptchaImageUtils.imageToBase64( image ) );
        captchaQuestion.setCaptchaId( captchaId );
        captchaQuestion.setCaptchaType( captchaType() );

        return ResponseData.success( new CaptchaData( captchaQuestion, captchaResult ) );
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
        // 两个数字比较 可以直接使用字符串比较
        if (!StringUtils.equalsIgnoreCase( answerData, captchaResult )) {
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
        return 1000;
    }

    /**
     * 随机颜色
     *
     * @param fc
     * @param bc
     * @return
     */
    private static Color randColor(int fc, int bc) {
        int r = fc + RANDOM.nextInt( bc - fc );
        int g = fc + RANDOM.nextInt( bc - fc );
        int b = fc + RANDOM.nextInt( bc - fc );
        return new Color( r, g, b );
    }

    /**
     * 将问题画到图上
     *
     * @param graphics
     * @return
     */
    private String drawCode(Graphics graphics) {
        StringBuilder questionStr = new StringBuilder();
        int result = 0;
        char operation = OP[ThreadLocalRandom.current().nextInt( 2 )];
        int firstNum = ThreadLocalRandom.current().nextInt( 100 );
        int secondNum = ThreadLocalRandom.current().nextInt( 100 );
        //处理负数问题。
        if (firstNum < secondNum) {
            int v = firstNum;
            firstNum = secondNum;
            secondNum = v;
        }
        questionStr.append( firstNum );
        questionStr.append( operation );
        questionStr.append( secondNum );
        questionStr.append( "=" );

        if (operation == '+') {
            result = firstNum + secondNum;
        } else if (operation == '-') {
            result = firstNum - secondNum;
        }

        graphics.setFont( FONTS[RANDOM.nextInt( FONTS.length )] );
        char[] question = questionStr.toString().toCharArray();
        for (int i = 0; i < question.length; i++) {
            String code = String.valueOf( question[i] );
            // 渐变
            AffineTransform fontAT = new AffineTransform();
            int rotate = RANDOM.nextInt( 30 );
            fontAT.rotate( RANDOM.nextBoolean() ? Math.toRadians( rotate ) : -Math.toRadians( (double) rotate / 2 ) );
            Font fx = new Font( FONTS[RANDOM.nextInt( FONTS.length )].getFontName(), RANDOM.nextInt( 5 ), 45 + RANDOM.nextInt( 8 ) ).deriveFont( fontAT );
            graphics.setFont( fx );

            graphics.setColor( randColor( 1, 255 ) );
            graphics.drawString( code, (i * WIDTH / question.length) + 3, HEIGHT / 2 + RANDOM.nextInt( 20 ) );

        }

        return String.valueOf( result );
    }

    /**
     * 混杂噪点。
     *
     * @param image
     */
    private void mixSource(BufferedImage image) {
        // 添加噪点。
        float yawpRate = 0.05f;
        int area = (int) (yawpRate * WIDTH * HEIGHT);
        for (int i = 0; i < area; i++) {
            int x = RANDOM.nextInt( WIDTH );
            int y = RANDOM.nextInt( HEIGHT );
            image.setRGB( x, y, RANDOM.nextInt( 255 ) );
        }
        // 添加混杂线条。
        Graphics graphics = image.getGraphics();
        for (int i = 0; i < LINE_NUM; i++) {
            int xs = RANDOM.nextInt( WIDTH );
            int ys = RANDOM.nextInt( HEIGHT );
            int xe = xs + RANDOM.nextInt( WIDTH );
            int ye = ys + RANDOM.nextInt( HEIGHT );
            graphics.setColor( randColor( 1, 255 ) );
            graphics.drawLine( xs, ys, xe, ye );
        }
    }


}
