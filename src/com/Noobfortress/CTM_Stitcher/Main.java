package com.Noobfortress.CTM_Stitcher;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import static com.Noobfortress.CTM_Stitcher.Main.TexturePart.getPartForCoords;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

public class Main {

    private static File regular, ctm, outputDir;

    public static void main(String[] args) {
        boolean useCtm;

        if (args.length != 0) {
            System.out.println("Command line arguments found. Using those for texture location(s)");

            List<String> arguments = Arrays.asList(args);

            if (!arguments.contains("-regular")) {
                System.out.println("Please select a regular texture");
                System.out.println("Format: -regular <file path> [-ctm <file path>] [-outputDir <file path>]");
            }
            regular = getFileTexture(args[arguments.indexOf("-regular") + 1]);

            if (arguments.contains("-ctm")) {
                ctm = getFileTexture(args[arguments.indexOf("-ctm") + 1]);
                useCtm = true;
            } else
                useCtm = false;

            if (arguments.contains("-outputDir"))
                outputDir = getCustomDir(args[arguments.indexOf("-outputDir") + 1]);
        } else {
            try(Scanner in = new Scanner(System.in)) {
                System.out.println("Use a single texture (1), or a normal and a ctm texture (2)");
                useCtm = getChoice(in) == 2;

                System.out.println("Select the path to the regular texture");
                regular =  getFileTexture(in);

                if (useCtm) {
                    System.out.println("Select the path to the CTM texture");
                    ctm = getFileTexture(in);
                }

                System.out.println("Use a custom output directory? (y/n)");
                outputDir = hasCustomDir(in) ? getCustomDir(in) : new File("");
            }
        }

        if (regular == null || (useCtm && ctm == null) || outputDir == null) return;

        BufferedImage regular = loadImage(Main.regular);
        Raster rasterRegular = Objects.requireNonNull(regular).getData();

        int regularWidth = rasterRegular.getWidth(), regularHeight = rasterRegular.getHeight();
        
        if (notPowerOfTwo(regularWidth) || notPowerOfTwo(regularHeight)) {
            System.out.println("Width/Height of regular image isn't a power of 2");
            return;
        }

        BufferedImage ctm = null;
        int ctmWidth = 0, ctmHeight = 0;
        if (useCtm) {
            ctm = loadImage(Main.ctm);
            Raster rasterCtm = Objects.requireNonNull(ctm).getData();

            ctmWidth = rasterCtm.getWidth();
            ctmHeight = rasterCtm.getHeight();

            if (notPowerOfTwo(ctmWidth) || notPowerOfTwo(ctmHeight)) {
                System.out.println("Width/Height of ctm image isn't a power of 2");
                return;
            }
        }

        System.out.println("Stitching texture...");

        BufferedImage out = new BufferedImage(regularWidth * 3, regularHeight * 3, TYPE_INT_ARGB);

        if (regularWidth != regularHeight) {
            //Different width & height implies animated texture
            //Each frame has to be stitched separately

            if (useCtm && (regularHeight / regularWidth != ctmHeight / ctmWidth)) {
                //Different amount of animated frames for regular and ctm texture
                System.out.println("Regular & ctm texture have differing amount of frames");
                return;
            }

            for (int frame = 0; frame < regularHeight / regularWidth; frame++) {
                if (useCtm) {
                    out = stitchImage(regular, ctm, regularWidth * frame);
                } else {
                    out = stitchImage(regular, regularWidth * frame);
                }
            }
        } else {
            if (useCtm) {
                out = stitchImage(regular, ctm);
            } else {
                out = stitchImage(regular);
            }
        }

        if (out == null)
            return;

        System.out.println("Texture stitched. Writing to disk...");
        writeImage(out);
    }

    private static int getChoice(Scanner in) {
        String input = in.next(); int choice;
        if (input.equalsIgnoreCase("one"))
            choice = 1;
        else if (input.equalsIgnoreCase("two"))
            choice = 2;
        else try {
            choice = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Please select a valid input (1 or 2)");
            return getChoice(in);
        }
        if (choice != 1 && choice != 2) {
            System.out.println("Please select a valid input (1 or 2)");
            return getChoice(in);
        }
        return choice;
    }

    private static File getFileTexture(Scanner in) {
        String path = in.next();

        if (!path.substring(path.length() - 3, path.length()).equalsIgnoreCase("png")) {
            System.out.println("Given path isn't a png, Please select a png file");
            return getFileTexture(in);
        }

        File raw = new File(path);
        File file = raw.isAbsolute() ? raw : new File(new File("").getAbsolutePath(), path);

        if (!(file.exists() || file.isDirectory())) {
            System.out.println("Given file doesn't exist. Please select a valid file");
            return getFileTexture(in);
        }

        return file;
    }

    private static File getFileTexture(String path) {
        if (!path.substring(path.length() - 3, path.length()).equalsIgnoreCase("png")) {
            System.out.println("Given path isn't a png, Please select a png file");
            return null;
        }

        File raw = new File(path);
        File file = raw.isAbsolute() ? raw : new File(new File("").getAbsolutePath(), path);

        if (!(file.exists() || file.isDirectory())) {
            System.out.println("Given file doesn't exist. Please select a valid file");
            return null;
        }

        return file;
    }

    private static boolean hasCustomDir(Scanner in) {
        String path = in.next();

        if (!path.equalsIgnoreCase("y") && !path.equalsIgnoreCase("n")) {
            System.out.println("Please select either \"y\" or \"n\"");
            return hasCustomDir(in);
        }

        else return path.equalsIgnoreCase("y");
    }

    private static File getCustomDir(Scanner in) {
        System.out.println("Please select a custom output directory:");
        String path = in.next();

        File raw = new File(path);
        File file = raw.isAbsolute() ? raw : new File(new File("").getAbsolutePath(), path);

        if (!file.exists() || !file.isDirectory()) {
            System.out.println("Please select a valid output directory");
            return getCustomDir(in);
        }

        return file;
    }

    private static File getCustomDir(String path) {
        File raw = new File(path);
        File file = raw.isAbsolute() ? raw : new File(new File("").getAbsolutePath(), path);

        if (!file.exists() || !file.isDirectory()) {
            System.out.println("Invalid output directory!\n");
            return null;
        }

        return file;
    }

    private static BufferedImage loadImage(File file) {
        try { return ImageIO.read(file); }
        catch (IOException e) { e.printStackTrace(); }
        return null;
    }

    private static void writeImage(BufferedImage out) {
        try {
            ImageIO.write(
                    out, "png",
                    new File(outputDir, Main.regular.getName().replace(".png", "") + "-3x3.png")
            );
            System.out.println("Image created");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Used for textures without ctm & animation
     *
     * @param regular The regular texture
     * @return A 3x3 grid of the original texture
     */
    private static BufferedImage stitchImage(BufferedImage regular) {
        return stitchImage(regular, 1);
    }

    /**
     * Used for textures without ctm, but with animation
     *
     * @param regular The regular texture
     * @param frameCount The amount of frames the texture has
     * @return 3x3 grids of the original texture, laid out in minecraft's animation format
     */
    private static BufferedImage stitchImage(BufferedImage regular, int frameCount) {
        Raster r = regular.getData();
        int width = r.getWidth(), height = r.getHeight();

        BufferedImage out = new BufferedImage(width * 3, height * 3, TYPE_INT_ARGB);

        for (int i = 0; i < frameCount; i++) {
            int[] rgb = regular.getRGB(0, i * width, width, width, null, 0, width);

            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    out.setRGB(width * x, width * (y + i * 3), width, width, rgb, 0, width);
                }
            }
        }
        return out;
    }

    /**
     * Used for textures with ctm, but without animation
     *
     * @param regular The regular texture
     * @param ctm The ctm texture
     * @return A stitched ctm texture emulating a 3x3 of blocks
     */
    private static BufferedImage stitchImage(BufferedImage regular, BufferedImage ctm) {
        return stitchImage(regular, ctm, 1);
    }

    /**
     * Used for textures with ctm & animation
     *
     * @param regular The regular texture
     * @param ctm The ctm texture
     * @param frameCount The amount of frames the texture has
     * @return Stitched ctm textures emulating a 3x3 grid of blocks, in minecraft's animation format
     */
    private static BufferedImage stitchImage(BufferedImage regular, BufferedImage ctm, int frameCount) {
        Raster r = regular.getData(), c = ctm.getData();

        int rWidth = r.getWidth(), rHeight = r.getHeight(), cWidth = c.getWidth();
        int pSize = rWidth / 2; //The width & height of a single part of a texture

        //5 textures split up into quarters. 5 * 4 = 20
        BufferedImage[] parts = new BufferedImage[20];

        for(TexturePart P : TexturePart.values())
            parts[P.ordinal()] = new BufferedImage(pSize, pSize, TYPE_INT_ARGB);

        BufferedImage out = new BufferedImage(rWidth * 3, rHeight * 3, TYPE_INT_ARGB);

        for (int i = 0; i < frameCount; i++) {
            int yBase = i * rWidth;

            //region Regular
            parts[0].setRGB( //Regular top left
                             0, 0, pSize, pSize,
                             regular.getRGB(0, yBase, pSize, pSize, null, 0, pSize),
                             0, pSize
            );

            parts[1].setRGB( //Regular top right
                             0, 0, pSize, pSize,
                             regular.getRGB(pSize, yBase, pSize, pSize, null, 0, pSize),
                             0, pSize
            );

            parts[2].setRGB( //Regular bottom left
                             0, 0, pSize, pSize,
                             regular.getRGB(0, pSize + yBase, pSize, pSize, null, 0, pSize),
                             0, pSize
            );

            parts[3].setRGB( //Regular bottom right
                             0, 0, pSize, pSize,
                             regular.getRGB(pSize, pSize + yBase, pSize, pSize, null, 0, pSize),
                             0, pSize
            );
            //endregion

            yBase = i * cWidth;
            int single = cWidth / 2;

            //region Edgeless
            parts[4].setRGB( //Edgeless top left
                             0, 0, pSize, pSize,
                             ctm.getRGB(0, yBase, pSize, pSize, null, 0, pSize),
                             0, pSize
            );

            parts[5].setRGB( //Edgeless top right
                             0, 0, pSize, pSize,
                             ctm.getRGB(pSize, yBase, pSize, pSize, null, 0, pSize),
                             0, pSize
            );

            parts[6].setRGB( //Edgeless bottom left
                             0, 0, pSize, pSize,
                             ctm.getRGB(0, pSize + yBase, pSize, pSize, null, 0, pSize),
                             0, pSize
            );

            parts[7].setRGB( //Edgeless bottom right
                             0, 0, pSize, pSize,
                             ctm.getRGB(pSize, pSize + yBase, pSize, pSize, null, 0, pSize),
                             0, pSize
            );
            //endregion

            //region Vertical
            parts[8].setRGB( //Vertical top left
                             0, 0, pSize, pSize,
                             ctm.getRGB(single, yBase, pSize, pSize, null, 0, pSize),
                             0, pSize
            );

            parts[9].setRGB( //Vertical top right
                             0, 0, pSize, pSize,
                             ctm.getRGB(pSize + single, yBase, pSize, pSize, null , 0, pSize),
                             0, pSize
            );

            parts[10].setRGB( //Vertical bottom left
                              0, 0, pSize, pSize,
                              ctm.getRGB(single, pSize + yBase, pSize, pSize, null, 0, pSize),
                              0, pSize
            );

            parts[11].setRGB( //Vertical bottom right
                             0, 0, pSize, pSize,
                              ctm.getRGB(pSize + single,pSize + yBase, pSize, pSize,null, 0 , pSize),
                              0, pSize
            );
            //endregion

            //region Horizontal
            parts[12].setRGB( //Horizontal top left
                              0, 0, pSize, pSize,
                              ctm.getRGB(0, single + yBase, pSize, pSize, null, 0, pSize),
                              0, pSize
            );

            parts[13].setRGB( //Horizontal top right
                              0, 0, pSize, pSize,
                              ctm.getRGB(pSize, single + yBase, pSize, pSize, null, 0, pSize),
                              0, pSize
            );

            parts[14].setRGB( //Horizontal bottom left
                              0, 0, pSize, pSize,
                              ctm.getRGB(0, pSize + single + yBase, pSize, pSize, null, 0, pSize),
                              0, pSize
            );

            parts[15].setRGB( //Horizontal bottom right
                             0, 0, pSize, pSize,
                              ctm.getRGB(pSize, pSize + single + yBase, pSize, pSize, null, 0, pSize),
                              0, pSize
            );
            //endregion

            //region Corners
            parts[16].setRGB( //Corner-only top left
                              0, 0, pSize, pSize,
                              ctm.getRGB(single, single + yBase, pSize, pSize, null, 0, pSize),
                              0, pSize
            );

            parts[17].setRGB( //Corner-only top right
                              0, 0, pSize, pSize,
                              ctm.getRGB(single + pSize, single + yBase, pSize, pSize, null, 0, pSize),
                              0, pSize
            );

            parts[18].setRGB( //Corner-only bottom left
                              0, 0, pSize, pSize,
                              ctm.getRGB(single, pSize + single + yBase, pSize, pSize, null, 0, pSize),
                              0, pSize
            );

            parts[19].setRGB( //Corner-only bottom right
                              0, 0, pSize, pSize,
                              ctm.getRGB(pSize + single, pSize + single + yBase, pSize, pSize, null, 0, pSize),
                              0, pSize
            );
            //endregion

            yBase = 3 * i * rWidth;
            for (int y = 0; y < 6; y++) {
                for (int x = 0; x < 6; x++) {
                    TexturePart P = getPartForCoords(x, y);
                    if (P == null) return null;
                    out.setRGB(
                            x * pSize, (y + yBase) * pSize,
                            pSize, pSize,
                            getRGB(parts[P.ordinal()]),
                            0, pSize
                    );
                }
            }
        }

        return out;
    }

    private static boolean notPowerOfTwo(int a) {
        return a <= 0 || (a & (a - 1)) != 0;
    }

    private static int[] getRGB(BufferedImage img) {
        return img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
    }

    /**
     * Makes keeping track of split textures easier
     */
    public enum TexturePart {
        REG_TOP_L, REG_TOP_R, REG_BOT_L, REG_BOT_R, //Regular textures
        NON_TOP_L, NON_TOP_R, NON_BOT_L, NON_BOT_R, //Edgeless textures
        VER_TOP_L, VER_TOP_R, VER_BOT_L, VER_BOT_R, //Vertical textures
        HOR_TOP_L, HOR_TOP_R, HOR_BOT_L, HOR_BOT_R, //Horizontal textures
        COR_TOP_L, COR_TOP_R, COR_BOT_L, COR_BOT_R; //Corner-only textures

        private static TexturePart getPartByName(String name) {
            for(TexturePart P : values()) {
                if (P.toString().equalsIgnoreCase(name))
                    return P;
            }
            return null;
        }

        //region Texture Arrangement
        /*
         * Texture Arrangement:
         * REG_TOP_L - HOR_TOP_R ----- HOR_TOP_L - HOR_TOP_R ----- HOR_TOP_L - REG_TOP_R
         *   |||         |||             |||         |||             |||         |||
         *   |||         |||             |||         |||             |||         |||
         * VER_BOT_L - NON_BOT_R ----- NON_BOT_L - NON_BOT_R ----- NON_BOT_L - VER_BOT_R
         *   |||         |||             |||         |||             |||         |||
         *   |||         |||             |||         |||             |||         |||
         *   |||         |||             |||         |||             |||         |||
         *   |||         |||             |||         |||             |||         |||
         * VER_TOP_L - NON_TOP_R ----- NON_TOP_L - NON_TOP_R ----- NON_TOP_L - VER_TOP_R
         *   |||         |||             |||         |||             |||         |||
         *   |||         |||             |||         |||             |||         |||
         * VER_BOT_L - NON_BOT_R ----- NON_BOT_L - NON_BOT_R ----- NON_BOT_L - VER_BOT_R
         *   |||         |||             |||         |||             |||         |||
         *   |||         |||             |||         |||             |||         |||
         *   |||         |||             |||         |||             |||         |||
         *   |||         |||             |||         |||             |||         |||
         * VER_TOP_L - NON_TOP_R ----- NON_TOP_L - NON_TOP_R ----- NON_TOP_L - VER_TOP_R
         *   |||         |||             |||         |||             |||         |||
         *   |||         |||             |||         |||             |||         |||
         * REG_BOT_L - HOR_BOT_R ----- HOR_BOT_L - HOR_BOT_R ----- HOR_BOT_L - REG_BOT_R
         *
         */
        //endregion
        public static TexturePart getPartForCoords(int x, int y) {
            if (x < 0 || x > 5 || y < 0 || y > 5) {
                System.err.printf(
                        "Incorrect lookup coordinates!\n" +
                        "Expected between (0,0) and (5,5). Got: (%d,%d)\n",
                        x, y
                );
                return null;
            }

            StringBuilder name = new StringBuilder();

            if ((x == 0 && y == 0) || (x == 5 && y == 0) || (x == 0 && y == 5) || (x == 5 && y == 5))
                name.append("REG");
            else if (x == 0 || x == 5)
                name.append("VER");
            else if (y == 0 || y == 5)
                name.append("HOR");
            else
                name.append("NON");
            name.append("_");

            name.append((y & 1) == 0 ? "TOP" : "BOT").append("_");
            name.append((x & 1) == 0 ? "L" : "R");

            return getPartByName(name.toString());
        }
    }
}
