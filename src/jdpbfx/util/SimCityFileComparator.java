package jdpbfx.util;

import java.io.File;
import java.io.FileFilter;
import java.util.Comparator;

/**
 * Comparator, prescribing the order by which the plugin files are loaded
 * into the game.
 *
 * @author jondor
 * @author memo
 */
public final class SimCityFileComparator implements Comparator<File> {

    public static final FileFilter DBPF_FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory())
                return true;
            else {
                String name = f.getName().toLowerCase();
                return name.endsWith(".dat")
                        || name.endsWith(".sc4lot")
                        || name.endsWith(".sc4model")
                        || name.endsWith(".sc4desc");
            }
        }
    };

    private final File appDir, appPluginDir, userPluginDir;
    private final FileSystem fileSystem;

    /**
     * Compares two files according to the order that is used by the game
     * to load plugin files. The implemented order is as follows:
     * <ol><li>
     * Files in the app dir are loaded first, files in the app plugin dir
     * afterwards. Files in the user plugin dir are loaded last.
     * </li><li>
     * Within each of those directories, .dat-files are generally loaded
     * after all the other DBPF files.
     * </li><li>
     * The files in the root of each folder are loaded before any subfolders.
     * </li><li>
     * The loading order of files in each folder, or of subfolders respectively,
     * is (assumed to be) <strong>determined by the file system</strong>, which
     * is why the file system, that is to be used for ordering, must be passed
     * at creation time.
     * </li>
     * </ol>
     * <p>
     * The currently supported file systems are:
     * <dl>
     * <dt>NTFS</dt>
     * <dd>Windows file system.
     * File names <i>usually</i> are case-insensitive, but the comparator
     * converts them to upper case for comparison. This is believed to be the
     * order used by the game on any Windows system. In particular, the
     * character '_' comes <strong>after</strong> 'Z'/'z'/any alpha-numeric
     * characters.</dd>
     * <dt>HFS+</dt>
     * <dd>Mac OS file system.
     * File names are generally case-insensitive. The order maintained
     * by the file system is the same as if the file names were converted
     * to lower case, which is therefore done by the comparator. In particular,
     * the character '_' comes <strong>before</strong> 'A'/'a'/any alphabetic
     * characters, but <strong>after</strong> '0'-'9'/numeric characters. It
     * is not yet tested whether the Aspyr port loads files in this order.</dd>
     * <dt>unspecified</dt>
     * <dd>It is attempted to choose a reasonable order, but this is dependent
     * on operating system and file system and therefore discouraged. The NTFS
     * parameter should be preferred.</dd>
     * </dl>
     * <p>
     * Notes:
     * <ul><li>
     * Linux file systems usually do not maintain a specific order
     * and are case-sensitive. This is not supported by this class.
     * </li><li>
     * The file system flag passed does not need to match the actual
     * underlying file system, but merely specifies the order implemented by
     * the comparator as defined above.
     * </li><li>
     * If the underlying file system supports case-sensitive file names, file
     * names are still treated case-insensitively by both NTFS and HFS+
     * comparators. Two files "a.dat" and "A.dat" might or might not return 0
     * when compared.
     * </li></ul>
     *
     * @param appDir
     *          the main directory of the SimCity 4 application. This is
     *          where the SimCity_1-5 dat files are expected to be found.
     * @param appPluginDir
     *          the plugin directory inside the app dir.
     * @param userPluginDir
     *          the user plugin directory, which is usually found inside the
     *          user documents folder.
     * @param fileSystem
     *          one of the constants {@link FileSystem#NTFS} or
     *          {@link FileSystem#HFS_PLUS} which affect the loading order,
     *          as explained above. Does not have to match the actual file
     *          system!
     */
    public SimCityFileComparator(File appDir, File appPluginDir, File userPluginDir, FileSystem fileSystem) {
        this.appDir = appDir;
        this.appPluginDir = appPluginDir;
        this.userPluginDir = userPluginDir;
        this.fileSystem = fileSystem;
    }

    @Override
    public int compare(File f1, File f2) {
        File file1 = f1, file2 = f2;
        if (fileSystem == FileSystem.HFS_PLUS) {
            file1 = new File(f1.getPath().toLowerCase());
            file2 = new File(f2.getPath().toLowerCase());
        } else if (fileSystem == FileSystem.NTFS) {
            file1 = new File(f1.getPath().toUpperCase());
            file2 = new File(f2.getPath().toUpperCase());
        } else if (f1.equals(new File(f1.getPath().toUpperCase()))) {
            // only if 'unspecified'
            file1 = new File(f1.getPath().toUpperCase());
            file2 = new File(f2.getPath().toUpperCase());
        }

        FileLocation fl1 = FileLocation.NOWHERE, fl2 = FileLocation.NOWHERE;
        File temp1 = file1, temp2 = file2;
        while((temp1 = temp1.getParentFile()) != null) {
            if(temp1.equals(userPluginDir)) {
                fl1 = FileLocation.USER_PLUGIN_DIR;
                break;
            }
            if(temp1.equals(appPluginDir)) {
                fl1 = FileLocation.APP_PLUGIN_DIR;
                break;
            }
/*
            if(temp1.equals(appSKUDir)) {
                fl1 = FileLocation.APP_SKU_DIR;
                break;
            }
            if(temp1.equals(appLocaleDir)) {
                fl1 = FileLocation.APP_LOCALE_DIR;
                break;
            }
*/
            if(temp1.equals(appDir) && !file1.getPath().substring(appDir.getPath().length() + 1).contains(File.separator)) {
                fl1 = FileLocation.APP_DIR;
                break;
            }
        }
        while((temp2 = temp2.getParentFile()) != null) {
            if(temp2.equals(userPluginDir)) {
                fl2 = FileLocation.USER_PLUGIN_DIR;
                break;
            }
            if(temp2.equals(appPluginDir)) {
                fl2 = FileLocation.APP_PLUGIN_DIR;
                break;
            }
/*
            if(temp2.equals(appSKUDir)) {
                fl1 = FileLocation.APP_SKU_DIR;
                break;
            }
            if(temp2.equals(appLocaleDir)) {
                fl1 = FileLocation.APP_LOCALE_DIR;
                break;
            }
*/
            if(temp2.equals(appDir) && !file2.getPath().substring(appDir.getPath().length() + 1).contains(File.separator)) {
                fl2 = FileLocation.APP_DIR;
                break;
            }
        }

        if(fl1.priority < fl2.priority)
            return -1;
        else if(fl1.priority > fl2.priority)
            return 1;
        else {
            boolean dat1 = false, dat2 = false;
            if(file1.getName().contains("."))
                dat1 = file1.getName().substring(file1.getName().lastIndexOf(".")).equalsIgnoreCase(".dat");
            if(file2.getName().contains("."))
                dat2 = file2.getName().substring(file2.getName().lastIndexOf(".")).equalsIgnoreCase(".dat");

            if(!dat1 && dat2)
                return -1;
            else if(dat1 && !dat2)
                return 1;
            else {
                File lcd = null;
                temp1 = file1;
                out: do {
                    while((temp1 = temp1.getParentFile()) != null) {
                        temp2 = file2;
                        while((temp2 = temp2.getParentFile()) != null) {
                            if(temp1.equals(temp2)) {
                                lcd = temp1;
                                break out;
                            }
                        }
                    }
                } while(false);
                if(lcd == null)
                    return 0;
                String s1 = file1.getPath().substring(lcd.getPath().length()),
                       s2 = file2.getPath().substring(lcd.getPath().length());
                int count1 = 0, count2 = 0, index1 = 0, index2 = 0;

                while((index1 = s1.indexOf(File.separator, index1)) != -1) {
                    count1++;
                    index1++;
                }
                while((index2 = s2.indexOf(File.separator, index2)) != -1) {
                    count2++;
                    index2++;
                }

                if(count1 == 1 && count2 != 1)
                    return -1;
                else if(count1 != 1 && count2 == 1)
                    return 1;
                else
                    return file1.getPath().compareTo(file2.getPath());
            }
        }
    }

    private enum FileLocation {
        APP_DIR(0),
        APP_LOCALE_DIR(1),
        APP_SKU_DIR(2),
        APP_PLUGIN_DIR(3),
        USER_PLUGIN_DIR(4),
        NOWHERE(Integer.MAX_VALUE);

        private int priority;

        FileLocation(int priority) {
            this.priority = priority;
        }
    }

    /**
     * Constants of supported file systems.
     */
    public enum FileSystem {
        /** Windows file system */
        NTFS,
        /** Mac OS file system */
        HFS_PLUS,
        /**
         * Unspecified file system.
         * This is discouraged. {@link #NTFS} should be used instead.
         */
        UNSPECIFIED;
    }
}