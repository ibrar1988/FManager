package root.fmanager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

class Utils {

    static Context context;

    static File[] sort (File[] files, boolean includeHidden) {
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
        });
        ArrayList<File> folderList = new ArrayList<>();
        ArrayList<File> fileList = new ArrayList<>();

        for (File temp : files) {
            if (includeHidden) {
                if (temp.isDirectory())
                    folderList.add(temp);
                else
                    fileList.add(temp);
            }
            else if (!temp.getName().startsWith(".")) {
                if (temp.isDirectory())
                    folderList.add(temp);
                else
                    fileList.add(temp);
            }
        }
        folderList.addAll(fileList);
        return folderList.toArray(new File[folderList.size()]);
    }

    @SuppressWarnings("FieldCanBeLocal")
    private static boolean success = true;
    static boolean delete (File fileToDelete) {
        if (fileToDelete.isDirectory() && fileToDelete.list().length > 0) {
            for (File file : fileToDelete.listFiles()) {
                delete(file);
            }
        }
        success = fileToDelete.delete();
        return success;
    }

    private static boolean copyFiles(File[] files, String copyTo) {
        try {
            for (final File file : files) {
                final boolean[] replaceAll = {false};

                final File copyToFile = new File(copyTo + File.separator + file.getName());

                final long fileLength = file.length();
                final long copyToFileLength = copyToFile.length();

                if (!replaceAll[0] && copyToFile.exists()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder existsDialog = new AlertDialog.Builder(context);
                            existsDialog.setTitle("Replace " + copyToFile.getName() + "?");
                            existsDialog.setMessage(
                                    "Old: " + copyToFileLength + " B" + '\n' +
                                            "New: " + fileLength + " B");

                            existsDialog.setMultiChoiceItems(new CharSequence[]{"replace all"},
                                    new boolean[]{false},
                                    new DialogInterface.OnMultiChoiceClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                            replaceAll[0] = isChecked;
                                        }
                                    });
                            existsDialog.setPositiveButton("Replace", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        if (fileLength < 2e+9)
                                            IOUtils.copy(new FileInputStream(file), new FileOutputStream(copyToFile));
                                        else IOUtils.copyLarge(new FileInputStream(file), new FileOutputStream(copyToFile));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } finally {
                                        notify();
                                    }
                                }
                            });
                            existsDialog.setNegativeButton("Skip", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                    notify();
                                }
                            });
                        }
                    }).wait();
                    continue;
                }
                if (fileLength < 2e+9)
                    IOUtils.copy(new FileInputStream(file), new FileOutputStream(copyToFile));
                else IOUtils.copyLarge(new FileInputStream(file), new FileOutputStream(copyToFile));
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    static boolean moveFiles (File[] files, String moveTo) {
        for (File file : files) {
            if (copyFiles(new File[]{file}, moveTo))
                //noinspection ResultOfMethodCallIgnored
                file.delete();
        }
        return true;
    }

    static String lastLineOfFile(File file ) {
        RandomAccessFile fileHandler = null;
        try {
            fileHandler = new RandomAccessFile( file, "r" );
            long fileLength = fileHandler.length() - 1;
            StringBuilder sb = new StringBuilder();

            for(long filePointer = fileLength; filePointer != -1; filePointer--) {
                fileHandler.seek( filePointer );
                byte readByte = fileHandler.readByte();

                if( ((int)readByte) == 0xA /*Line Feed (LF)*/) {
                    if( filePointer == fileLength ) {
                        continue;
                    }
                    break;
                } else if( ((int)readByte) == 0xD /*Carriage Return (CR)*/) {
                    if( filePointer == fileLength - 1 ) {
                        continue;
                    }
                    break;
                }
                sb.append( IOUtils.toString(new byte[]{readByte}, "ISO_8859_1") );
            }
            return sb.reverse().toString();
        } catch( FileNotFoundException e ) {
            e.printStackTrace();
            return null;
        } catch( IOException e ) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (fileHandler != null )
                    fileHandler.close();
            } catch (IOException ignored) {}
        }
    }
}
