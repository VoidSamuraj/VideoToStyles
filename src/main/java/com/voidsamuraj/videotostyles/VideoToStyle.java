package com.voidsamuraj.videotostyles;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiConsumer;


public class VideoToStyle {
    public enum TypeOfStyle{
        ASCII,
        DOTS
    }
    public static void main(String[] args)  {
        //loading native library OpenCv
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    /**
     * Applies selected style to OpenCv image
     * @param src - source file
     * @param out - output file
     *
     * @param tos - type of used style
     * @param color - true when want to change character color
     * @param frameDivider - process 1/frameDivider frames
     * @param width - width of container for char in px for TypeOfStyle.ASCII or size of dots size for TypeOfStyle.DOTS
     * @param height - height of container for char in px for TypeOfStyle.ASCII
     * @param fontScale - font scale(too big may overlap other chars) for TypeOfStyle.ASCII
     * @param isShifting - if you want to shift lines of dots using TypeOfStyle.DOTS
     */
    public static void processVideo(String src, String out, int frameDivider, TypeOfStyle tos, boolean color, int width, int height, double fontScale, boolean isShifting, BiConsumer<Double,Double> updateProgress){

        File inputFile=new File(src);
        File temp=new File("output_no_sound.mp4");
        File outputFile=new File(out);

        VideoCapture capture=new VideoCapture(inputFile.getAbsolutePath());

        int frame_width = (int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int frame_height = (int) capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);

        double fps=capture.get(Videoio.CAP_PROP_FPS);
        int frames= (int)capture.get(Videoio.CAP_PROP_FRAME_COUNT);

        VideoWriter writer = new VideoWriter(
                temp.getAbsolutePath(),
                VideoWriter.fourcc('P', 'I', 'M', '1'),     //MPEG-1
                fps/frameDivider,
                new Size(frame_width, frame_height)
        );


        Mat frame = new Mat();
        int i=0;

        while (capture.read(frame)) {
            ++i;

            updateProgress.accept((double) i  , (double)frames);

            if(i%frameDivider==0){

                if(tos== TypeOfStyle.ASCII)
                    VideoToStyle.asciiImage(frame,color,width,height,fontScale);
                else
                    VideoToStyle.dotsImage(frame,color,isShifting,width);

                writer.write( frame);
            }
        }

        capture.release();
        writer.release();

        //ffmpeg ask if override file, delete to less complicated command
        if(outputFile.exists())
            outputFile.delete();


        try{
            // Create a process for the FFmpeg command
            ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg", "-i", temp.getAbsolutePath(), "-i", inputFile.getAbsolutePath(), "-c", "copy", "-map", "0:v:0", "-map", "1:a:0", outputFile.getAbsolutePath());
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process process = processBuilder.start();

            while(true) {
                try {
                    process.exitValue();
                    break;
                } catch (IllegalThreadStateException e) {
                    Thread.sleep(200);
                }
            }
            temp.delete();


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public static Mat writableImageToMat(Image image) throws IOException {
        File temp=new File("temp_image_toAscii_convert_image.jpg");
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        BufferedImage imageRGB = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.OPAQUE);
        Graphics2D graphics = imageRGB.createGraphics();
        graphics.drawImage(bufferedImage, 0, 0, null);
        ImageIO.write(imageRGB, "jpg", temp);
        Mat mat=Imgcodecs.imread(temp.getAbsolutePath());
        temp.delete();
        return  mat;
    }
    public static Image fromMatToImage(Mat mat) {
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".png", mat, matOfByte);
        byte[] byteArray = matOfByte.toArray();
        InputStream in = new ByteArrayInputStream(byteArray);
        BufferedImage bufImage;
        try {
            bufImage = ImageIO.read(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return SwingFXUtils.toFXImage(bufImage, null);
    }

    /**
     * Applies ascii style to image
     * @param matrix - edited matrix(image)
     * @param color - true when want to change character color
     * @param xSize - width of container for char in px
     * @param ySize - height of container for char in px
     * @param fontScale - font scale(too big may overlap other chars)
     */
    public static void asciiImage(Mat matrix, boolean color, int xSize, int ySize, double fontScale){
        applyStyle(matrix, TypeOfStyle.ASCII,color,xSize,ySize,fontScale,false,1);
    }

    /**
     * Applies dots style to image
     * @param matrix - edited matrix(image)
     * @param color - true when want to change character color
     * @param isShifting - if you want to shift lines of dots
     * @param scale - scale of dots size
     */
    public static void dotsImage(Mat matrix, boolean color,boolean isShifting, int scale){
        applyStyle(matrix, TypeOfStyle.DOTS,color,0,0,1,isShifting,scale);
    }
    /**
     * Applies selected style to OpenCv image
     * @param matrix - edited matrix(image)
     * @param type - type of used style
     * @param color - true when want to change character color
     * @param xSize - width of container for char in px for TypeOfStyle.ASCII
     * @param ySize - height of container for char in px for TypeOfStyle.ASCII
     * @param fontScale - font scale(too big may overlap other chars) for TypeOfStyle.ASCII
     * @param isShifting - if you want to shift lines of dots using TypeOfStyle.DOTS
     * @param scale - scale of dots size for TypeOfStyle.DOTS
     */
    private static void applyStyle(Mat matrix, TypeOfStyle type, boolean color, int xSize, int ySize, double fontScale, boolean isShifting, int scale)
    {
        //we work directly on provided Mat, need to copy to read unchanged image data
        Mat matrixCp=matrix.clone();
        //fill new image with color
        matrix.setTo((color|| TypeOfStyle.DOTS==type)?new Scalar(0, 0, 0):new  Scalar(185, 184, 181));

        Scalar paintColor= TypeOfStyle.ASCII==type?new Scalar(0, 0, 0):new  Scalar(255, 255, 255);

        int height=matrix.height();
        int width=matrix.width();
        double[] p;
        int r,g,b;
        int luminance;
        int dziel;

        //for DOTS
        boolean shift=false;
        int size= scale*2;
        for (int y = 0; y < height; y += ((type== TypeOfStyle.ASCII)? ySize : size )) {
            for (int x = 0; x < width; x +=((type== TypeOfStyle.ASCII)? xSize : size )) {
                r=g=b=0;
                dziel=1;
                for (int yy = y; (yy < y +((type== TypeOfStyle.ASCII)? ySize : size ))&&yy<height; yy++) {
                    for (int xx = x; (xx < x + ((type== TypeOfStyle.ASCII)? xSize : size ))&&xx<width; xx++) {
                        p=matrixCp.get(y,x);
                        r+=(int)p[0];
                        g+=(int)p[1];
                        b+=(int)p[2];
                        ++dziel;
                    }
                }
                r/=dziel;
                g/=dziel;
                b/=dziel;

                if(r>255)r=255;
                if(g>255)g=255;
                if(b>255)b=255;

                luminance=(r+g+b)/3;

                if(color)
                    paintColor=new Scalar(r,g,b);
                switch (type) {
                    case ASCII ->
                            Imgproc.putText(matrix, String.valueOf(getAsciiChar(luminance, color)), new Point(x, y), Imgproc.FONT_HERSHEY_PLAIN, fontScale, paintColor, 1);
                    case DOTS -> {
                        int radius=Math.round(luminance / 255f * scale);
                        if (isShifting) {
                            if (shift)
                                Imgproc.circle(matrix, new Point(x+scale , y+scale ),radius, paintColor, -1);
                            else
                                Imgproc.circle(matrix, new Point(x + size, y +scale), radius, paintColor, -1);

                        } else
                            Imgproc.circle(matrix, new Point(x + scale, y + scale), radius, paintColor, -1);
                    }
                }

            }
            shift=!shift;
        }

    }

    /**
     * Function returning char by brightness
     * @param luminance - value between 0-255
     * @param color - if true reverts chars array
     * @return - luminance-dependent char from a predefined array
     */
    private static char getAsciiChar(int luminance, boolean color)
    {
        String s="##@%=+*:-. ";
        if(color)
            s=" .-:*=%@##";
        int l=s.length();

        if(luminance<0)return s.charAt(0);
        else if(luminance>=255)
            return s.charAt(l-1);
        else
            return s.charAt((int)((luminance*l/255.0f)));
    }


}
