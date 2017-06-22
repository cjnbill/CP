package cop5618;

import java.awt.image.*;
import java.util.Hashtable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;


public class FJBufferedImage extends BufferedImage {

	/**
	 * Constructors
	 */

	public FJBufferedImage(int width, int height, int imageType) {
		super(width, height, imageType);
	}

	public FJBufferedImage(int width, int height, int imageType, IndexColorModel cm) {
		super(width, height, imageType, cm);
	}

	public FJBufferedImage(ColorModel cm, WritableRaster raster, boolean isRasterPremultiplied,
						   Hashtable<?, ?> properties) {
		super(cm, raster, isRasterPremultiplied, properties);
	}


	/**
	 * Creates a new FJBufferedImage with the same fields as source.
	 *
	 * @param source
	 * @return
	 */
	public static FJBufferedImage BufferedImageToFJBufferedImage(BufferedImage source) {
		Hashtable<String, Object> properties = null;
		String[] propertyNames = source.getPropertyNames();
		if (propertyNames != null) {
			properties = new Hashtable<String, Object>();
			for (String name : propertyNames) {
				properties.put(name, source.getProperty(name));
			}
		}
		return new FJBufferedImage(source.getColorModel(), source.getRaster(), source.isAlphaPremultiplied(), properties);
	}

	@Override
	public void setRGB(int xStart, int yStart, int w, int h, int[] rgbArray, int offset, int scansize) {
		/****IMPLEMENT THIS METHOD USING PARALLEL DIVIDE AND CONQUER*****/
        ForkJoinPool pool = new ForkJoinPool();
        RGBTask rt = new RGBTask(rgbArray, 0, h, w, true, pool.getParallelism());
        pool.invoke(rt);
    }


	@Override
	public int[] getRGB(int xStart, int yStart, int w, int h, int[] rgbArray, int offset, int scansize) {
		/****IMPLEMENT THIS METHOD USING PARALLEL DIVIDE AND CONQUER*****/
        ForkJoinPool pool = new ForkJoinPool();
        RGBTask rt = new RGBTask(rgbArray, 0, h, w, false, pool.getParallelism());
        pool.invoke(rt);
		return rgbArray;
	}


	public class RGBTask extends RecursiveAction {
		private int[] mSource;
		private int mStart;
		private int mLength;
		private int mWidth = 15; // Processing window size, should be odd.
		private boolean isSet;
		private int sThreshold;
		private int off = 0;

		public RGBTask(int[] src, int start, int length, int width, boolean isset, int sth) {
			mSource = src;
			mStart = start;
			mLength = length;
			mWidth = width;
			off = start* mWidth;
			isSet = isset;
			sThreshold = sth;
		}

		@Override
		protected void compute() {
			if (mLength < sThreshold) {
			    try {
                    if(isSet)
                        FJBufferedImage.super.setRGB(0, mStart, mWidth,mLength, mSource, off, mWidth);
                    else
                        FJBufferedImage.super.getRGB(0, mStart, mWidth,mLength, mSource, off, mWidth);
                    return;
                }
                catch (Exception e){
                    System.out.println(this.off);
                }
			}

			int split = mLength / 2;

			invokeAll(new RGBTask(mSource, mStart, split, mWidth, isSet, sThreshold),
					new RGBTask(mSource, mStart + split, mLength - split, mWidth,
                            isSet, sThreshold));
		}

	}
}
