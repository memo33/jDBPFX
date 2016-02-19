package jdbpfx.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by acq on 2/18/16.
 */
public class DBPFPackagerTest {

    public static void main(String[] args) {

        byte[] original = new byte[5];
        Arrays.fill(original, (byte) 2);

        byte[] data = createSampleArray(10);

        DBPFPackager packager = new DBPFPackager();

        byte[] returned = packager.arrayCopy2(data, 0, original, 0, 10);

        System.out.println("DBPFPackager.arrayCopy2 Test");
        System.out.println("data Array: " + describeArray(data));
        System.out.println("original Array: " + describeArray(original));
        System.out.println("returned Array: " + describeArray(returned));

        byte[] returned2 = packager.offsetCopy(data, 0, 8, 5);

        System.out.println("DBPFPackager.offsetCopy Test");
        System.out.println("data Array: " + describeArray(data));
        System.out.println("returned2 Array: " + describeArray(returned2));
    }

    private static byte[] createSampleArray(int size) {
        byte[] result = new byte[size];

        for (int i = 0; i < size; i++)
            result[i] = (byte) i;

        return result;
    }

    private static String describeArray(byte[] array) {
        StringBuilder elements = new StringBuilder();

        elements.append('{');

        String sep = "";
        for (byte element : array) {
            elements.append(sep);
            elements.append(String.format("%02x", element));
            sep = ", ";
        }

        elements.append('}');

        return String.format(" objectid=%d, size=%d, elements=%s", System.identityHashCode(array), array.length, elements.toString());
    }
}
