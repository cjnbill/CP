package cop5618;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ColorHistEq {

    //Use these labels to instantiate you timers.  You will need 8 invocations of now()
	static String[] labels = { "getRGB", "convert to HSB", "create brightness map",
			"parallel prefix", "probability array", "equalize pixels", "setRGB" };

	static Timer colorHistEq_serial(BufferedImage image, BufferedImage newImage) {
		Timer times = new Timer(labels);
		/**
		 * IMPLEMENT SERIAL METHOD
		 */
		ColorModel colorModel = ColorModel.getRGBdefault();
		int w = image.getWidth();
		int h = image.getHeight();
		times.now();
		//getRGB
		int[] sourcePixelArray = image.getRGB(0, 0, w, h, new int[w * h], 0, w);
		times.now();

		//convert to HSB"
		float [][]HSBPixelArray=new float[sourcePixelArray.length][3];
		for (int i = 0; i < sourcePixelArray.length; i++) {
			HSBPixelArray[i][0] = sourcePixelArray[i];
		}
		HSBPixelArray =
				Arrays.stream(HSBPixelArray)
				.map(pixel->R2H(colorModel.getRed((int)pixel[0]),colorModel.getGreen((int)pixel[0]),colorModel.getBlue((int)pixel[0])))
				.toArray(float[][]::new);
		times.now();

		//create brightness map
		int numBins=100;
		NavigableMap<Double, Integer> Group = setGroup(numBins);
		double[] brightnessArray =
				Arrays.stream(HSBPixelArray)
				.mapToDouble(b -> (double)b[2]).toArray();
		Stream<Double> ds =
				Arrays.stream(brightnessArray).boxed();
		Map<Integer, Long> result =
				ds.collect(Collectors.groupingBy(item->Group.floorEntry(item).getValue(), Collectors.counting()));
		Map<Integer, Long> treeMap = new TreeMap<>(result);
		times.now();

		//parallel prefix
		long[] prearr = treeMap.entrySet().stream().mapToLong(item->item.getValue()).toArray();
		Arrays.parallelPrefix(prearr, Long::sum);
		times.now();

		//probability array
		long sum = prearr[prearr.length-1];
		double[] pros=Arrays.stream(prearr).mapToDouble(item->item/(double)sum).toArray();
		times.now();

		//equalize pixels
		Arrays.stream(HSBPixelArray)
				.forEach(item->item[2]=(float)(pros[Group.floorEntry((double)item[2]).getValue()]));
		int[] destPixelArray  = Arrays.stream(HSBPixelArray)
				.mapToInt(item->Color.HSBtoRGB(item[0],item[1],item[2])).toArray();
		times.now();

		//setRGB
		newImage.setRGB(0, 0, w, h, destPixelArray, 0, w);
		times.now();
		return times;
	}

	static NavigableMap<Double, Integer> setGroup(int numBins) {
	    numBins=numBins>256?256:numBins;
		NavigableMap<Double, Integer> map = new TreeMap<Double, Integer>();
		for (int i = 0; i < numBins; i++) {
			map.put(i * (1.0 / numBins), i);
		}
		return map;
	}

	static float[] R2H(int r, int g, int b){
		float[] a=new float[3];
		Color.RGBtoHSB(r,g,b,a);
		return a;
	}



	static Timer colorHistEq_parallel(FJBufferedImage image, FJBufferedImage newImage) {
		Timer times = new Timer(labels);
		/**
		 * IMPLEMENT SERIAL METHOD
		 */
		ColorModel colorModel = ColorModel.getRGBdefault();
		int w = image.getWidth();
		int h = image.getHeight();
		times.now();

		//getRGB
		int[] sourcePixelArray = image.getRGB(0, 0, w, h, new int[w * h], 0, w);
		times.now();

		//convert to HSB
		float [][]HSBPixelArray=new float[sourcePixelArray.length][3];
		for (int i = 0; i < sourcePixelArray.length; i++) {
			HSBPixelArray[i][0] = sourcePixelArray[i];
		}
		HSBPixelArray =
				Arrays.stream(HSBPixelArray).parallel()
						.map(pixel->R2H(colorModel.getRed((int)pixel[0]),colorModel.getGreen((int)pixel[0]),colorModel.getBlue((int)pixel[0])))
						.toArray(float[][]::new);
		times.now();

		//create brightness map
		int numBins=100;
		NavigableMap<Double, Integer> Group = setGroup(numBins);
		double[] brightnessArray = Arrays.stream(HSBPixelArray).parallel()
				.mapToDouble(b -> (double)b[2]).toArray();
		Stream<Double> ds = Arrays.stream(brightnessArray).boxed();
		Map<Integer, Long> result = ds.parallel().collect(Collectors.
				groupingBy(item->Group.floorEntry(item).getValue(), Collectors.counting()));
		Map<Integer, Long> treeMap = new TreeMap<>(result);
		times.now();

		//parallel prefix
		long[] prearr=treeMap.entrySet().stream().parallel().mapToLong(item->item.getValue()).toArray();
		Arrays.parallelPrefix(prearr, Long::sum);
		times.now();

		//probability array
		long sum = prearr[prearr.length-1];
		double[] pros=Arrays.stream(prearr).parallel().mapToDouble(item->item/(double)sum).toArray();
		times.now();

		//equalize pixels
		Arrays.stream(HSBPixelArray)
				.parallel()
				.forEach(item->item[2]=(float)(pros[Group.floorEntry((double)item[2]).getValue()]));
		int[] destPixelArray  = Arrays.stream(HSBPixelArray).parallel()
				.mapToInt(item->Color.HSBtoRGB(item[0],item[1],item[2])).toArray();
		times.now();

		//setRGB
		newImage.setRGB(0, 0, w, h, destPixelArray, 0, w);
		times.now();
		return times;
	}

}
