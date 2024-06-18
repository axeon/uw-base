package uw.mfa.captcha.strategy;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uw.common.dto.ResponseData;
import uw.mfa.captcha.CaptchaStrategy;
import uw.mfa.captcha.util.CaptchaImageUtils;
import uw.mfa.captcha.util.CaptchaJsonUtils;
import uw.mfa.captcha.util.CaptchaRandomUtils;
import uw.mfa.captcha.vo.CaptchaData;
import uw.mfa.captcha.vo.CaptchaPoint;
import uw.mfa.captcha.vo.CaptchaQuestion;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * 点选文字验证码
 */
public class ClickWordCaptchaStrategy implements CaptchaStrategy {

    private static final Logger log = LoggerFactory.getLogger( ClickWordCaptchaStrategy.class );

    /**
     * 点选的文字内容 随机字母
     */
    private static final String WORDS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * 文字大小
     */
    private static final int FONT_SIZE = 25;

    /**
     * 字体大小一半
     */
    private static final int FONT_SIZE_HALF = FONT_SIZE / 2;

    /**
     * 点选文字 字体颜色是否随机
     */
    private static final boolean FONT_COLOR_RANDOM = Boolean.TRUE;

    /**
     * 点选文字字体大小
     */
    private static final int CLICK_WORD_SIZE = 30;
    /**
     * 点选文字字体
     */
    private static final Font FONT_STYLE = new Font( "", Font.BOLD, CLICK_WORD_SIZE );
    /**
     * 点选文字 字体总个数
     */
    private static final int WORD_TOTAL_COUNT = 5;

    @Override
    public ResponseData<CaptchaData> generate(String captchaId) {
        BufferedImage mainImage = CaptchaImageUtils.getSlideMainImage();
        if (null == mainImage) {
            return ResponseData.errorMsg( "ClickWordPuzzle main image load failed!" );
        }
        try {

            CaptchaQuestion captchaQuestion = new CaptchaQuestion();
            ArrayList<String> wordList = new ArrayList<>();
            ArrayList<CaptchaPoint> pointList = new ArrayList<>();

            Graphics mainImageGraphics = mainImage.getGraphics();
            int width = mainImage.getWidth();
            int height = mainImage.getHeight();

            //定义随机1到arr.length某一个字不参与校验
            int num = CaptchaRandomUtils.getRandomInt( 1, WORD_TOTAL_COUNT );
            Set<String> currentWords = getRandomWords( WORD_TOTAL_COUNT );
            // 用map保存 方便乱序
            Map<String, CaptchaPoint> map = new HashMap<>(currentWords.size());
            int i = 0;
            for (String word : currentWords) {
                //随机字体坐标
                CaptchaPoint point = randomWordPoint( width, height, i, WORD_TOTAL_COUNT );
                //随机字体颜色
                if (FONT_COLOR_RANDOM) {
                    mainImageGraphics.setColor( new Color( CaptchaRandomUtils.getRandomInt( 1, 255 ),
                            CaptchaRandomUtils.getRandomInt( 1, 255 ), CaptchaRandomUtils.getRandomInt( 1, 255 ) ) );
                } else {
                    mainImageGraphics.setColor( Color.BLACK );
                }
                //设置角度
                AffineTransform affineTransform = new AffineTransform();
                affineTransform.rotate( Math.toRadians( CaptchaRandomUtils.getRandomInt( -45, 45 ) ), 0, 0 );
                Font rotatedFont = FONT_STYLE.deriveFont( affineTransform );
                mainImageGraphics.setFont( rotatedFont );
                mainImageGraphics.drawString( word, point.getX(), point.getY() );

                if ((num - 1) != i) {
                    map.put(word, point);
                }
                i++;
            }

            // HashMap存储根据key值hash已经乱序
            for (Map.Entry<String, CaptchaPoint> entry : map.entrySet()) {
                wordList.add( entry.getKey() );
                pointList.add( entry.getValue() );
            }

            captchaQuestion.setMainImageBase64( CaptchaImageUtils.imageToBase64( mainImage ) );
            captchaQuestion.setCaptchaId( captchaId );
            captchaQuestion.setCaptchaType( captchaType() );
            captchaQuestion.setSubData( CaptchaJsonUtils.toJSONString(wordList));

            // 存储的验证信息
            String captchaResult = CaptchaJsonUtils.toJSONString( pointList );

            return ResponseData.success( new CaptchaData( captchaQuestion, captchaResult ) );
        } catch (Exception e) {
            return ResponseData.errorMsg( "ClickWordPuzzle captcha generate failed! " + e.getMessage() );
        }
    }

    @Override
    public ResponseData verify(String answerData, String captchaResult) {
        CaptchaPoint[] pointResult = null;
        CaptchaPoint[] pointAnswer = null;
        /**
         * [
         *             {
         *                 "x": 85.0,
         *                 "y": 34.0
         *             },
         *             {
         *                 "x": 129.0,
         *                 "y": 56.0
         *             },
         *             {
         *                 "x": 233.0,
         *                 "y": 27.0
         *             }
         * ]
         */
        try {
            pointResult = CaptchaJsonUtils.parseArray( captchaResult, CaptchaPoint[].class );
            pointAnswer = CaptchaJsonUtils.parseArray( answerData, CaptchaPoint[].class );
        } catch (Exception e) {
            return ResponseData.errorMsg( "ClickWordPuzzle point format invalid! " + e.getMessage() );
        }
        for (int i = 0; i < pointResult.length; i++) {
            // 如果点选大于字体一半
            if (Math.abs( pointResult[i].x - pointAnswer[i].x ) > FONT_SIZE || Math.abs( pointResult[i].y - pointAnswer[i].y ) > FONT_SIZE) {
                return ResponseData.error();
            }
        }

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
     * 随机字体循环排序下标
     *
     * @param imageWidth    图片宽度
     * @param imageHeight   图片高度
     * @param wordSortIndex 字体循环排序下标(i)
     * @param wordCount     字数量
     * @return
     */
    private static CaptchaPoint randomWordPoint(int imageWidth, int imageHeight, int wordSortIndex, int wordCount) {
        int avgWidth = imageWidth / (wordCount + 1);
        int x, y;
        if (avgWidth < FONT_SIZE_HALF) {
            x = CaptchaRandomUtils.getRandomInt( 1 + FONT_SIZE_HALF, imageWidth );
        } else {
            if (wordSortIndex == 0) {
                x = CaptchaRandomUtils.getRandomInt( 1 + FONT_SIZE_HALF, avgWidth * (wordSortIndex + 1) - FONT_SIZE_HALF );
            } else {
                x = CaptchaRandomUtils.getRandomInt( avgWidth * wordSortIndex + FONT_SIZE_HALF, avgWidth * (wordSortIndex + 1) - FONT_SIZE_HALF );
            }
        }
        y = CaptchaRandomUtils.getRandomInt( FONT_SIZE, imageHeight - FONT_SIZE );
        return new CaptchaPoint( x, y );
    }

    /**
     * 获取随机文字
     *
     * @param wordCount
     * @return
     */
    private Set<String> getRandomWords(int wordCount) {
        Set<String> words = new HashSet<>();
        int size = WORDS.length();
        do {
            String t = String.valueOf( WORDS.charAt( CaptchaRandomUtils.getRandomInt( size ) ) );
            words.add( t );
        } while (words.size() < wordCount);
        return words;
    }


}
