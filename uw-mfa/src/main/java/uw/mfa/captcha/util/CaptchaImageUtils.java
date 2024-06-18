package uw.mfa.captcha.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 图片存储类
 */
public class CaptchaImageUtils {

    private static final Logger log = LoggerFactory.getLogger( CaptchaImageUtils.class );

    /**
     * slide主图。
     */
    private static final List<BufferedImage> slideMainImageCache = new ArrayList<>();

    /**
     * slide图块。
     */
    private static final List<BufferedImage> slideJigsawImageCache = new ArrayList<>();

    /**
     * 旋转主图。 (滑块需要底图色彩分明 故和通用底图分开)
     */
    private static final List<BufferedImage> rotateMainImageCache = new ArrayList<>();

    // 静态块初始化图片
    static {
        // 初始化缺块
        // 方形缺块
        slideJigsawImageCache.add( base64ToImage( "iVBORw0KGgoAAAANSUhEUgAAADIAAAAyCAYAAAAeP4ixAAACm0lEQVR42u2ZW0sbQRTH0wd9qQ8++QG0D/0c1s+gT" +
                "/WteWqFQkJuVVfUxJa2UYuRRkxNNGrFa0AqXmravDVFpYUiIgilVBD8AD5Nz3/dUQtpMrPGzgiz8Ceb3XOW89szlzOzHs81jmAw2Dydes0G+i0WCYds4RzXwuHwA4" +
                "/uRyAQaB0ZjDICqaRt2OoK8ZwHmhiKsc2VJDsovmfHP5Zt4RzXcI" +
                "/bwUcriFAoFOPBrS29ZSf7K2UFG24PX10y0c6DKnxIVYTggu2VzLSr5rgjk4lymcGzlFFQAC0IYnR4QBqCC74OTIvKZpVEEFu5Mdcg8HWaV1JlJz9EEAfFOdcg8HU6" +
                "/aHKpnWGIDC8ugWBr9O0zm4kyJmJuMjEZuvX96VqgAiJMpeQfdtCD555F3cNwYVnyMC4ArlukNWUATEgBsSAGBADogzE3jiIWmyaZt" +
                "+v25NagFCZcnrlN096allW3b8q20Sp8mCE1tyFtZQSkEqlDC0FukrCEGUtREb3SF4y3OROC9k3SmB2Pk2yn98W7XP84v9cZuivotLn8zWIrEE6VMOU0u7nKTaRfClXIZNxG4fJr45rA3O0N8" +
                "/SlzC9oqvDjsuRLacNzF4he9HMqFvUC8Ekhs8324ofM1oN07PpQQ7zWDQrj+CQTcW1AsGLdZrXuhCI3+9vgkOsv1srkKPdeZ6R30IgztBsT5o6gUhvaOgKIp0RMmyEQ7SvW8s+QtoQ7ewP4ZAZe3W7R61RZ" +
                "/j9otHwKz2PUN31RLcJETM7WofwzK5jiYJMpGVqLdVF405esvo92V9mXq+3hi7epZv3MYvzPqEKwtV6pNwmtqrm5GqFyJ06n0XYi1iPXU+dj045s/lgQAyIATEgBqRqH0NV6MY+T/9PyX6e" +
                "/gOIYk8wSb1cowAAAABJRU5ErkJgglN6" ) );
        // 七角星
        slideJigsawImageCache.add( base64ToImage( "iVBORw0KGgoAAAANSUhEUgAAADIAAAAyCAYAAAAeP4ixAAAACXBIWXMAAA7EAAAOxAGVKw4bAAAC" +
                "+UlEQVRoge3ZTcgVVRzH8Y8v3dSwFxUCiQzJ3tRNIGobiR5atBChjSkGRoESCBJBIIgvEK4UE6JFLVMjaCFBbRJUKIxWWYoPQvRCVBoUkWKp0+J/r8/1eu/MvXfO3HmC5wv/1cw55/ebM+ec//yHKab4XzO" +
                "/6gGmVz0AnsSLIxinct7Gt3WLKMsc/IEMq2vWUorNwkSG92rWUorPTRj5C3PrlTMcy0yYaMUrtSoakoNuN/JlrYqG4E787nYjGZZXMeCMguuPYC+u43vc6LPfF7Cxx7V/8Wmf/UzDKryKBi702a4r75tYrB" +
                "+Kw21eQZsTus9GJmaqkdN2NtaKXe6XZpszmDm0gyYP4nKHmGtNsa/h4Y77H80x0YoNHW3ux0s41mWsDGNlTbTYXSDsHPaJQ29/H0Y+w1K8gS/EK9vr3mOpTMBd" +
                "+LEPganjqlinSdlUg5EDqU0QO8jpEZq4hHurMAJPyX+fU8bWqky0ODwCE99IsN0WsUj3LTJlPFu1iRZ7KzTx8ahMwAOqm5Vkh18Rz+C7ikxk" +
                "+BMvi12yEmbjLZGeVL3YM3GiL0xtYoVIQ0ZhoD0uYn0KA3dgj0gVRm2iPY4ozrh78gS+qtlAe/yE5wYxMF2k51cmgfjOuIF3RBKby2KcnASCi+KCSJdu0vmpuxDj+Bo/iFmZ04w6uYKzOIWPxJfqb" +
                "/hVGOt7r56Px7BErJ0leBwPiUJDCq6LdXBePPFz4qGOi3pBlmicrjRwXPlX5Wd9vPd5lK3G/4NZJfsgZvzvMh2k+K2Q4t9HQ8l1OFmMwIIyjVMYua/g+hl80Ec/lf/VyuMe+Yv4pDA6A4cK7n16xNpvYXEPURk" + "+ERlzO3ty7n9+NJK7s6qHqKN6l0VfF2dGZ5stVYvNY6yLoIOKi+Obxdbd3m5HdTKLWd8hZtcAbde6NTF9M7W4QdjWFHEN24doPyaq" + "/BneTahrYHaKj67O6vogrBQJYKliddlzpIF1omg3LKexRhQdamPoz88u3J2wrymmSMV/UxeJ0zK3LDUAAAAASUVORK5CYII=" ) );
        // 月亮
        slideJigsawImageCache.add( base64ToImage( "iVBORw0KGgoAAAANSUhEUgAAADIAAAAyCAYAAAAeP4ixAAAACXBIWXMAAA7EAAAOxAGVKw4bAAAC4klEQVRogc3aT4gWdRzH8dc" +
                "+WmIWS64o2VZGreJF0DUvgoqHCDoVHSIIhKJLt8LAm3VVL169iH8WTEUvHfKwlZeokJQNtgg0MqmUElctrM3p8NuR5dl5/szM75mZN3zguTzf+Xx+M" +
                "/M83993hji0MI73cAoXMYNpbI90jIHyAg7iFyQddFkI2kh2YlJn8+1aVY/NzjyHM/oPkODrWpx24S3cli/ET1hdg9dMHsYx+QIkuIHna/CbyTA" +
                "+kz9Egldr8JvJML5VLMREDX4zWYYLioX4B2urt5zNKcVCJDhZg99M3lU8RIIXq7e8kFHcUjzEXSyu3HUGE8qdjW+qt7yQTZhVLsiJyl3PMb+Z" +
                "+xCLSta7VfL7hUmDjOKlCPXuRahRiDTILnFu0mURahQiDfJKpHpPRqpTiKdwX7mbPNXVir0/oIUdGIpUbxTrI9XKRQtbItd8LXK9vvlCnMsq1RVhD1MpLayMXHMN3o5csy+ui3tGEvwh/gL15E4E41k6U2UI" +
                "+DeS8Sx9NCDPjwp93V/CBnCM/JORvNoTOcTj+LLtGJfg+wEHSYRd40iEEBvwXUb9GThXQZAEv+J1xcanw9iLvzvUvggHKgqSahrv4+k+AozP+bvZo+bHQ3gTR/IsT0R+wBR" +
                "+Fv4Glghz4jHhMup3ZvzBkNCxXhWv36qDbemHojOsJuh3LE5vvNORVqYOTgqzBoRr8Z76Vzev7mNje7IiE/e6Nbng" +
                "/AgbokG2K4PQjqwgcLgB5vpV16Z0BX5rgMlemsGz3YIQJiqxhhGD0ju9QqTsb4DZTjrabwh4CJ82wHS7vhLamFw8gvMNMJ9qCsvzhpgf5mwDQlwQYQbQwj71/QAcwdKyIebzsrA5qirATbwRM8B8RnBI" +
                "+QdC3fQfjgsz6YGzee5gMRvNWXyCrVUEaOcJ7Bb2zEUDXBKelq0rYyTmrvAZ4SWzLcJbRGuEn8vHhEd6t/GnMBv+URjpfI5rMQ7+PxzLBIuszVN+AAAAAElFTkSuQmCC" ) );
        // 正方形
        slideJigsawImageCache.add( base64ToImage(
                "iVBORw0KGgoAAAANSUhEUgAAADIAAAAyCAYAAAAeP4ixAAAACXBIWXMAAA7EAAAOxAGVKw4bAAAA6klEQVRoge2awQnCQAAE586PIOLHEmwpZVmDHQgiJA9LSCmaFoyPS1DjKxq4jezA/ne43H2y8CQABVACV" + "+AOtGK5d93KrmtgwAqoBIqOzQXYvJ7ESaDUtyn7kykEyvyaYgHsgR3zZh1Il2ebu8mP3ALpJfi4" + "/TOjDaRvbPbE3AWmwiJqWEQNi6hhETUsooZF1LCIGhZRwyJqWEQNi6hhETUsooZF1LCIGhZRwyJqRP7jH2IbgSZ3iwloIlDnbjEBdQQOuVtMwAHSWOBM/j3Jt6l4GTwsgaNAqbE5kSZab" + "/TDswr94VnFYHj2AAVYSd3CFa4FAAAAAElFTkSuQmCC" ) );
        // 三角形
        slideJigsawImageCache.add( base64ToImage(
                "iVBORw0KGgoAAAANSUhEUgAAADIAAAAyCAYAAAAeP4ixAAAACXBIWXMAAA7EAAAOxAGVKw4bAAACFElEQVRoge3YT4gOcRzH8ZfFPhQJrXBxVGzSytVFDohEDk4O3FzUluTO0XGTpNy1LkrS5s/JRS4U2sSFOKx" + "/hUX9HKbJ9uw888w8zzPzG/W863OZnmY+7+b3fGfmRzWcwgf8whscqug6lXIYoS0/sCtmqbKswGuLRQIeRexVmguyJdIcjVetOJvwVb7ISyyPVbAo1" + "+VLpDkTq2ARJvBHMZGPWBOnZnceKiaR5lKcmvkcV04i4Du2xCjbiRZmlRcJuBGhb0fO600iSP5TE/VXXsxGfNG7SMBM7a0zuKY/iTQH6i6+kJ34nVGqlzzDsnrr/+N+gYJlcrre+gnHeiybl3dY1U" + "+ppSV/38I01vVz0QxWS5bqgwGftyPnDP5upPmGzXVIbMDnCkWCZBJWztWKJYJkeY1XKbHD4MZtt9ypUmSmJok0e6uQOFKzRMBT5SdqLqN4FUEk4OQgRSYjSQS8xcpBSIzhU0SRINmV6ZsrkSWC5Lk11o" + "/EuGS7M7ZIwFQ/IvcaIJBmHlt7kcjau42dW2UlRvGiAcWzsqeMyNkGFO6Ux1hSRGI95hpQOC8niohMNaBot8xKPu46sl1zxm23TOaJ3G1AwaKZw9osiYMNKFc2l7NEnjegWNn81LYJPoJtWXYNp4V9Cw" + "+MSF4B/kfetx+4KP5SKZvb2r4g0yflbuzXYRo0iHk8wU2J0JAhQ4YMGTIQ/gJj/diXF1HPGwAAAABJRU5ErkJggg==" ) );
        // 五角星
        slideJigsawImageCache.add( base64ToImage(
                "iVBORw0KGgoAAAANSUhEUgAAADIAAAAyCAYAAAAeP4ixAAAACXBIWXMAAA7EAAAOxAGVKw4bAAACxUlEQVRoge3ZS2hUZxjG8d8ksZBaRKO0BS0mLkpddKH2olDqZVUXxl0hK6VdWkQKFemmuHRhXRSli9KF0C6kLRSqgoK71o0ki64avF+ahBgEL9TaNuPizMHpJDNzvnO+mTML//AwyfDN+z7PzOHwnu+js+zFXVzHtg736hjr8QTVmmYxVKqjnHzjWYhUn5fqKAdDeGhhkGsYKNFXMActDJFqtERfQfTjiuZBzpVnLYxRzUNU8R9eL81dAOe0DlLFl6W5y8gbmNc+yD28GLNxX8xi+ASVDOuWYyxy72gsw33tf41U4+XYbM9+2UOk2lKK0xZUMCk8yMkyzLZip/AQVTzGqhL8NuW0fEGqkimgMFnuMPACXq7p1drrK3V/jwXUauQ+LmEaM5iqvU7XvTcrua03pSIZKbZjpM5gqvT/FQWMxuBfSZjpRTSOX+GU/JdFr+iHSi1tf5TvrTxu9+F82S4i8C2sxk3lXx559TOWpInWYKIHTIXq+/oQKUvxSw+Yy6pjjQHqGcDXPWCylebxWasQ9RyS3M3KNt2ov+UY/3fjUQ+YT/UQH4SGSNmEWz0Q4k9syBsi5TXJGFBWiMtYVzREylLFJty8uiiZ9aIygK+6GOKMyBsU9fThQZeCbA41FsJGvBT4mby8E7I4NMh7geuL8G7I4l4OEnRphTKju3etlVmNhfwi6yWPvt3krawLQ4JszWGkKB0J8n4OI0V5uxNF8z5F/lVTns/eiR1iOKeRHyVz0gh+ylljdcwgHwU2n7D4ufoO/B5YK+qZ43cZm07jY623l/qxD3MZax6OGeRGm2aPcURyRpKVIRzHP21qn42SQPI80qrRGcUON9/EhRb1pwrU/h97mjSYxK5YTfAhrjbptTZGgwMNRefwqWSHPjaD+MLCvYIoM94gTuAPybFy5vmnAMM4it8k21ODXej5nOcsxlOkiBxQ4ZsTBgAAAABJRU5ErkJggg==" ) );
        // 梯形
        slideJigsawImageCache.add( base64ToImage(
                "iVBORw0KGgoAAAANSUhEUgAAADIAAAAyCAYAAAAeP4ixAAAACXBIWXMAAA7EAAAOxAGVKw4bAAABu0lEQVRoge2ZS1ICMRRFT0MjHwXkXyCCWi7NNThwLW7JnVgOnWk5eATSIa0YitCvKmf2Uj24p" +
                        "/IheUAikUhoIisZHwPDmEH+wQfw7g7mJR8/AS/AxSkTBfIKPAOf9qBPpAHMgG6EUCFMkWwFkZrnwytgFCNRIEMkYwGfSJdqi4zxrBafSA+YnDxOOBMkY4GypVXVEwsk20EzUnWRFjIrheyuSIbsj" +
                        "+tIoUJZ45y4rkgdWFL" +
                        "+Q1kVbvlDJAduosUJZ8EBIstoccJZIqtni09kHS1OOCuc65PWpdXHOVldkTZyl9HAyi5ckRVyadTAg124InfxchzNvV24Iho2uqGQ1be0tPDrHnmMGORYFsClKWwRLUevoYGV1xYZ4Xl5VZgGMDeFLbJEz9ELknV7nXJFqtg1KaNUZIG+GVmYwhaZo0skR9pWDdiJZMhjRZNIBgzYXB6NiOll+d7wVaaHI+LtTCigh8xKQWSvV6SAvRkZIY8VbfTZdEWNyACdM9Jhs7dtEY17pI68aJs1dn8jNM8aKZw50Kkh7/QZ1W/KlTED2jWkl6ql4eBjCrRyROQLeDtvnmC+EYdEIpFIxOcHX6ARq05z/30AAAAASUVORK5CYII=" ) );

        // 初始化底图。
        String SLIDE_BASE = "/uw/mfa/captcha/slideImg/";
        String ROTATE_BASE = "/uw/mfa/captcha/rotateImg/";

        for (int i = 0; i < 10; i++) {
            loadImageFromResource( slideMainImageCache, SLIDE_BASE + i + ".png" );
            loadImageFromResource( rotateMainImageCache, ROTATE_BASE + i + ".png" );
        }
    }

    /**
     * 获取滑块。
     *
     * @return
     */
    public static BufferedImage getSlideJigsawImage() {
        return slideJigsawImageCache.get( CaptchaRandomUtils.getRandomInt( slideJigsawImageCache.size() ) );
    }

    /**
     * 获取Slide底图。
     *
     * @return
     */
    public static BufferedImage getSlideMainImage() {
        return deepCopy( slideMainImageCache.get( CaptchaRandomUtils.getRandomInt( slideMainImageCache.size() ) ) );
    }

    /**
     * 获取旋转底图。
     *
     * @return
     */
    public static BufferedImage getRotateMainImage() {
        return deepCopy( rotateMainImageCache.get( CaptchaRandomUtils.getRandomInt( rotateMainImageCache.size() ) ) );
    }

    /**
     * BufferedImage对象深拷贝
     *
     * @param original
     * @return
     */
    public static BufferedImage deepCopy(BufferedImage original) {
        if (original == null) {
            return null;
        }
        ColorModel cm = original.getColorModel();
        return new BufferedImage( cm, original.copyData( null ), cm.isAlphaPremultiplied(), null );
    }

    /**
     * 图片转base64 字符串 注意默认png格式
     *
     * @param templateImage
     * @return
     */
    public static String imageToBase64(BufferedImage templateImage) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write( templateImage, "png", byteArrayOutputStream );
        } catch (IOException e) {
            log.error( "imageToBase64 Exception: {}", e.getMessage(), e );
            return null;
        }
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.getEncoder().encodeToString( bytes );
    }

    /**
     * base64 字符串转图片
     *
     * @param base64String
     * @return
     */
    public static BufferedImage base64ToImage(String base64String) {
        try {
            byte[] bytes = Base64.getDecoder().decode( base64String );
            ByteArrayInputStream inputStream = new ByteArrayInputStream( bytes );
            return ImageIO.read( inputStream );
        } catch (IOException e) {
            log.error( "base64ToImage Exception: {}", e.getMessage(), e );
        }
        return null;
    }

    /**
     * 旋转角度 逆时针旋转degree度数
     *
     * @param originalImage
     * @param degree
     * @return
     */
    public static BufferedImage rotateImage(BufferedImage originalImage, double degree) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        BufferedImage rotatedImage = new BufferedImage( width, height, originalImage.getType() );
        Graphics2D g2d = rotatedImage.createGraphics();

        // 设置渲染提示，选择抗锯齿的插值方法 旋转后图片失帧严重必须处理抗锯齿
        // 设置“抗锯齿”的属性
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g2d.setRenderingHint( RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT );
        g2d.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
        g2d.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC );

        // 定义旋转转换
        AffineTransform at = new AffineTransform();
        at.rotate( Math.toRadians( degree ), (double) width / 2, (double) height / 2 );
        at.translate( (double) (width - originalImage.getWidth()) / 2, (double) (height - originalImage.getHeight()) / 2 );

        // 应用转换
        g2d.setTransform( at );
        g2d.drawImage( originalImage, 0, 0, null );
        g2d.dispose();

        return rotatedImage;
    }

    /**
     * 按照templateImage模板从mainImage的x和y的部分切出一张新的图层, 并且mainImage图层生成了遮罩层
     *
     * @param mainImage     原图
     * @param templateImage 模板图
     * @param x             随机扣取坐标X
     * @param y             随机扣取坐标y
     * @throws Exception
     */
    public static BufferedImage cutByTemplate(BufferedImage mainImage, BufferedImage templateImage, int x, int y) {
        //生成新的拼图图像
        BufferedImage newJigsawImage = new BufferedImage( templateImage.getWidth(), templateImage.getHeight(), templateImage.getType() );
        Graphics2D g2d = newJigsawImage.createGraphics();
        // 设置“抗锯齿”的属性
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g2d.setRenderingHint( RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT );
        g2d.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
        g2d.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC );

        //如果需要生成RGB格式，需要做如下配置,Transparency 设置透明
        newJigsawImage = g2d.getDeviceConfiguration().createCompatibleImage( templateImage.getWidth(), templateImage.getHeight(), Transparency.TRANSLUCENT );

        //临时数组遍历用于高斯模糊存周边像素值
        int[][] matrix = new int[3][3];
        int[] values = new int[9];

        int xLength = templateImage.getWidth();
        int yLength = templateImage.getHeight();
        // 模板图像宽度
        for (int i = 0; i < xLength; i++) {
            // 模板图片高度
            for (int j = 0; j < yLength; j++) {
                // 如果模板图像当前像素点不是透明色 copy源文件信息到目标图片中
                int rgb = templateImage.getRGB( i, j );
                if (rgb < 0) {
                    newJigsawImage.setRGB( i, j, mainImage.getRGB( x + i, y + j ) );

                    //抠图区域高斯模糊
                    readPixel( mainImage, x + i, y + j, values );
                    fillMatrix( matrix, values );
                    mainImage.setRGB( x + i, y + j, avgMatrix( matrix ) );
                }

                //防止数组越界判断
                if (i == (xLength - 1) || j == (yLength - 1)) {
                    continue;
                }
                int rightRgb = templateImage.getRGB( i + 1, j );
                int downRgb = templateImage.getRGB( i, j + 1 );
                //描边处理，,取带像素和无像素的界点，判断该点是不是临界轮廓点,如果是设置该坐标像素是白色
                if ((rgb >= 0 && rightRgb < 0) || (rgb < 0 && rightRgb >= 0) || (rgb >= 0 && downRgb < 0) || (rgb < 0 && downRgb >= 0)) {
                    newJigsawImage.setRGB( i, j, Color.white.getRGB() );
                    mainImage.setRGB( x + i, y + j, Color.white.getRGB() );
                }
            }
        }

        int bold = 5;
        g2d.setStroke( new BasicStroke( bold, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL ) );
        g2d.drawImage( newJigsawImage, 0, 0, null );
        g2d.dispose();

        return newJigsawImage;

    }

    /**
     * 按照templateImage模板将mainImage的x和y的部分生成遮罩层
     *
     * @param mainImage     原图
     * @param templateImage 模板图
     * @param x             随机扣取坐标X
     * @param y             随机扣取坐标y
     * @throws Exception
     */
    public static void addLacuna(BufferedImage mainImage, BufferedImage templateImage, int x, int y) {
        //临时数组遍历用于高斯模糊存周边像素值
        int[][] matrix = new int[3][3];
        int[] values = new int[9];

        int xLength = templateImage.getWidth();
        int yLength = templateImage.getHeight();
        // 模板图像宽度
        for (int i = 0; i < xLength; i++) {
            // 模板图片高度
            for (int j = 0; j < yLength; j++) {
                // 如果模板图像当前像素点不是透明色 copy源文件信息到目标图片中
                int rgb = templateImage.getRGB( i, j );
                if (rgb < 0) {
                    //抠图区域高斯模糊
                    readPixel( mainImage, x + i, y + j, values );
                    fillMatrix( matrix, values );
                    mainImage.setRGB( x + i, y + j, avgMatrix( matrix ) );
                }

                //防止数组越界判断
                if (i == (xLength - 1) || j == (yLength - 1)) {
                    continue;
                }
                int rightRgb = templateImage.getRGB( i + 1, j );
                int downRgb = templateImage.getRGB( i, j + 1 );
                //描边处理，,取带像素和无像素的界点，判断该点是不是临界轮廓点,如果是设置该坐标像素是白色
                if ((rgb >= 0 && rightRgb < 0) || (rgb < 0 && rightRgb >= 0) || (rgb >= 0 && downRgb < 0) || (rgb < 0 && downRgb >= 0)) {
                    mainImage.setRGB( x + i, y + j, Color.white.getRGB() );
                }
            }
        }

    }

    /**
     * 加载图片
     *
     * @param imageFile 文件路径
     * @return
     */
    private static void loadImageFromResource(List<BufferedImage> imageList, String imageFile) {
        try {
            InputStream is = CaptchaImageUtils.class.getResourceAsStream( imageFile );
            if (is != null) {
                imageList.add( ImageIO.read( CaptchaImageUtils.class.getResourceAsStream( imageFile ) ) );
            }
        } catch (Exception e) {
            log.error( "加载图片资源[{}]失败! 异常：{}", imageFile, e.getMessage(), e );
        }
    }

    private static void readPixel(BufferedImage img, int x, int y, int[] pixels) {
        int xStart = x - 1;
        int yStart = y - 1;
        int current = 0;
        for (int i = xStart; i < 3 + xStart; i++) {
            for (int j = yStart; j < 3 + yStart; j++) {
                int tx = i;
                if (tx < 0) {
                    tx = -tx;

                } else if (tx >= img.getWidth()) {
                    tx = x;
                }
                int ty = j;
                if (ty < 0) {
                    ty = -ty;
                } else if (ty >= img.getHeight()) {
                    ty = y;
                }
                pixels[current++] = img.getRGB( tx, ty );

            }
        }
    }

    private static void fillMatrix(int[][] matrix, int[] values) {
        int filled = 0;
        for (int[] x : matrix) {
            for (int j = 0; j < x.length; j++) {
                x[j] = values[filled++];
            }
        }
    }

    private static int avgMatrix(int[][] matrix) {
        int r = 0;
        int g = 0;
        int b = 0;
        for (int[] x : matrix) {
            for (int j = 0; j < x.length; j++) {
                if (j == 1) {
                    continue;
                }
                Color c = new Color( x[j] );
                r += c.getRed();
                g += c.getGreen();
                b += c.getBlue();
            }
        }
        return new Color( r / 8, g / 8, b / 8 ).getRGB();
    }

}
