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

import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 滑动Captcha
 */
public class SlidePuzzleCaptchaStrategy implements CaptchaStrategy {

    private static final Logger log = LoggerFactory.getLogger( SlidePuzzleCaptchaStrategy.class );

    /**
     * 随机生成器
     */
    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    /**
     * 滑动验证可接受误差
     */
    protected static int SLIP_OFFSET = 5;

    @Override
    public ResponseData<CaptchaData> generate(String captchaId) {
        //原生图片
        BufferedImage mainImage = CaptchaImageUtils.getSlideMainImage();
        if (null == mainImage) {
            return ResponseData.errorMsg( "SlidePuzzle main image load failed!" );
        }
        //抠图图片
        BufferedImage jigsawImage = CaptchaImageUtils.getSlideJigsawImage();
        if (null == jigsawImage) {
            return ResponseData.errorMsg( "SlidePuzzle jigsaw image load failed!" );
        }
        try {
            CaptchaQuestion captchaQuestion = new CaptchaQuestion();

            int mainWidth = mainImage.getWidth();
            int mainHeight = mainImage.getHeight();
            int jigsawWidth = jigsawImage.getWidth();
            int jigsawHeight = jigsawImage.getHeight();

            // 随机生成拼图坐标 坐标为切块顶部和左侧坐标
            CaptchaPoint point = generateJigsawPoint( mainWidth, mainHeight, jigsawWidth, jigsawHeight );
            int x = point.getX();
            int y = point.getY();

            // 新建的图像根据模板颜色赋值,源图生成遮罩
            BufferedImage newJigsawImage = CaptchaImageUtils.cutByTemplate( mainImage, jigsawImage, x, y );

            // 增加识别难度的缺块
            // 将滑块微微旋转10角度以内
            BufferedImage rotateImage = CaptchaImageUtils.rotateImage( jigsawImage, CaptchaRandomUtils.getRandomInt( 6, 10 ) );
            // 参数的算法可以将两个阴影块错位开来
            CaptchaImageUtils.addLacuna( mainImage, rotateImage, generateLacunaPoint( mainWidth, jigsawWidth, x ), y );


            captchaQuestion.setMainImageBase64( CaptchaImageUtils.imageToBase64( mainImage ) );
            captchaQuestion.setSubImageBase64( CaptchaImageUtils.imageToBase64( newJigsawImage ) );
            captchaQuestion.setCaptchaId( captchaId );

            // 传输编码
            captchaQuestion.setCaptchaType( captchaType() );
            captchaQuestion.setSubData( JsonUtils.toString( new CaptchaPoint( 0, y ) ) );
            String captchaResult = JsonUtils.toString( point );
            return ResponseData.success( new CaptchaData( captchaQuestion, captchaResult ) );
        } catch (Exception e) {
            return ResponseData.errorMsg( "SlidePuzzle captcha generate failed! " + e.getMessage() );
        }
    }

    @Override
    public ResponseData verify(String answerData, String captchaResult) {
        CaptchaPoint pointResult = null;
        CaptchaPoint pointAnswer = null;
        try {
            // 后端存储的Captcha答案
            pointResult = JsonUtils.parse( captchaResult, CaptchaPoint.class );
            // 前端的回答
            pointAnswer = JsonUtils.parse( answerData, CaptchaPoint.class );
        } catch (Exception e) {
            return ResponseData.errorMsg( "SlidePuzzle point format invalid! " + e.getMessage() );
        }
        if (Math.abs( pointResult.x - pointAnswer.x ) > SLIP_OFFSET || pointResult.y != pointAnswer.y) {
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
        return 1200;
    }

    /**
     * 随机生成拼图坐标
     *
     * @param mainWidth
     * @param mainHeight
     * @param jigsawWidth
     * @param jigsawHeight
     * @return
     */
    private static CaptchaPoint generateJigsawPoint(int mainWidth, int mainHeight, int jigsawWidth, int jigsawHeight) {
        int widthDifference = mainWidth - jigsawWidth;
        int heightDifference = mainHeight - jigsawHeight;
        int x, y = 0;
        if (widthDifference <= 0) {
            x = 5;
        } else {
            x = RANDOM.nextInt( mainWidth - jigsawWidth );
        }
        if (heightDifference <= 0) {
            y = 5;
        } else {
            y = RANDOM.nextInt( mainHeight - jigsawHeight );
        }
        return new CaptchaPoint( x, y );
    }

    /**
     * 随机生成干扰图拼图坐标
     *
     * @param mainWidth
     * @param jigsawWidth
     * @return
     */
    private static int generateLacunaPoint(int mainWidth, int jigsawWidth, int x) {
        int half = mainWidth / 2;
        // 10算是对旋转后图片误差的兼容
        if (x > half) {
            return CaptchaRandomUtils.getRandomInt( half - jigsawWidth + 10 );
        } else {
            return CaptchaRandomUtils.getRandomInt( half + jigsawWidth + 10, mainWidth - jigsawWidth - 10 );
        }
    }
}
