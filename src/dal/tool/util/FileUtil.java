package dal.tool.util;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 파일의 관련 조작 및 문자열 변환을 쉽게 하기 위한 Util 클래스
 * @author 권영달
 *
 */
public class FileUtil {


	private static String fileSizeUnit[] = { "B", "KB", "MB", "GB", "TB", "PB" };

	/**
	 * 주어진 파일 경로에 대한 디렉토리를 생성한다. 필요한 하위 디렉토리가 모두 생성된다. 
	 * @param filepath 파일 경로
	 */
	public static void createFileDirectory(String filepath) {
		String path = filepath.replaceAll("\\\\", "/");		
		int idx = path.lastIndexOf("/");
		String dirName = path;
		if(idx > -1) {
			dirName = path.substring(0, idx);
		}
		File dir = new File(dirName);
		createDirectory(dir);
	}

	/**
	 * 주어진 파일 경로에 대한 디렉토리를 생성한다. 필요한 하위 디렉토리가 모두 생성된다. 
	 * @param file File 객체
	 */
	public static void createFileDirectory(File file) {
		String filepath = file.getAbsolutePath().replaceAll("\\\\", "/");
		createFileDirectory(new File(filepath));
	}

	/**
	 * 주어진 경로에 대한 디렉토리를 생성한다. 필요한 하위 디렉토리가 모두 생성된다. 
	 * @param dirpath 디렉토리 경로
	 */
	public static void createDirectory(String dirpath) {
		File dir = new File(dirpath);
		createDirectory(dir);
	}

	/**
	 * 주어진 경로에 대한 디렉토리를 생성한다. 필요한 하위 디렉토리가 모두 생성된다. 
	 * @param directory 디렉토리를 나타내는 File 객체
	 */
	public static void createDirectory(File directory) {
		if(!directory.exists()) {
			directory.mkdirs();
		}
	}

	/**
	 * 주어진 파일 또는 디렉토리를 삭제한다.
	 * @param fileOrDirectoryPath 삭제할 파일 또는 디렉토리 경로
	 * @param recursive 하위파일 또는 디렉토리까지 모두 삭제하려면 true
	 */
	public static void deleteDirectory(String fileOrDirectoryPath, boolean recursive) {
		String path = fileOrDirectoryPath.replaceAll("\\\\", "/");		
		deleteDirectory(new File(path), recursive);
	}

	/**
	 * 주어진 파일 또는 디렉토리를 삭제한다.
	 * @param fileOrDirectory 삭제할 파일 또는 디렉토리를 나타내는 File 객체
	 * @param recursive 하위파일 또는 디렉토리까지 모두 삭제하려면 true
	 */
	public static void deleteDirectory(File fileOrDirectory, boolean recursive) {
	    if(recursive && fileOrDirectory.isDirectory()) {
	        for(File child : fileOrDirectory.listFiles()) {
	            deleteDirectory(child, true);
	        }
	    }
	    fileOrDirectory.delete();
	}
	
    /**
     * 파일의 크기에 단위를 붙여서 문자열로 표시한다.
     * @param size 파일의 크기(단위:byte)
     * @return 단위를 포함한 파일 크기 문자열
     */
    public static String humanReadableSize(long size) {
        double dSize = size;
        int i;
        for(i = 0; i < 4; i++) {
            if(dSize < 1024D)
                break;
            dSize /= 1024D;
        }
        dSize = Math.round(dSize * 100d) / 100d;
        return (new StringBuilder(String.valueOf(dSize))).append(fileSizeUnit[i]).toString();
    }
    
    /**
     * 파일 경로 구분자를 통일한다.
     * @param filePath 파일 경로
     * @return 구분자가 통일된 파일 경로
     */
    public static String convertFileSeparator(String filePath) {
    	if(File.separator.equals('/')) {
        	if(filePath.indexOf('\\') > -1) {
            	filePath = filePath.replaceAll("\\\\", "/");
        	}
    	} else {
        	if(filePath.indexOf('/') > -1) {
            	filePath = filePath.replaceAll("/", "\\\\");
        	}
    	}
    	return filePath;
    }

    /**
     * 파일 경로를 절대 경로로 변경한다.
     * @param filePath 파일 경로
     * @return 파일 절대 경로
     * @exception 주어진 경로에 대한 절대 경로를 가지고 오면서 발생한 모든 Exception
     */
    public static String convertFileAbsolutePath(String filePath) throws IOException {
		return new File(filePath).getCanonicalPath();
    }    

    /**
     * List 객체를 파일로 기록한다.
     * @param f 저장될 파일 객체
     * @param objs 저장할 List 객체
     * @exception 메소드 수행시 발생한 모든 Exception
     */
	public static void writeObjectFile(File f, List<Object> objs) throws Exception {
		if(f == null || objs == null)
			return;
		writeObjectFile(new FileOutputStream(f), objs);
	}

    /**
     * List 객체를 스트림에 기록한다.
     * @param os 저장될 OutputStream 객체
     * @param objs 저장할 List 객체
     * @exception 메소드 수행시 발생한 모든 Exception
     */
	public static void writeObjectFile(OutputStream os, List<Object> objs) throws Exception {
		if(os == null)
			return;
		if(objs == null) {
			try {
				os.close();
			} catch(Exception e) {
			}
			return;
		}
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(os);
			for(Object o : objs) {
				out.writeObject(o);
			}
			out.flush();
		} catch(Exception e) {
			throw e;
		} finally {
			try {
				out.close();
			} catch(Exception e2) {
			}
		}
	}

    /**
     * 파일을 읽어서 List 객체로 로딩한다.
     * @param f 로딩할 파일 객체
     * @return 로딩된 List 객체
     * @exception 메소드 수행시 발생한 모든 Exception
     */
	public static List<Object> readObjectFile(File f) throws Exception {
		if(f == null)
			return null;
		return readObjectFile(new FileInputStream(f));
	}

    /**
     * 스트림을 읽어서 List 객체로 로딩한다.
     * @param is 로딩할 InputStream 객체
     * @return 로딩된 List 객체
     * @exception 메소드 수행시 발생한 모든 Exception
     */
	public static List<Object> readObjectFile(InputStream is) throws Exception {
		if(is == null)
			return null;
		List<Object> objs = new ArrayList<Object>();
		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(is);
			Object obj = null;
			while((obj = in.readObject()) != null) {
				objs.add(obj);
			}
		} catch(EOFException eof) {
		} catch(Exception e) {
			throw e;
		} finally {
			try {
				in.close();
			} catch(Exception e2) {
			}
		}
		return objs;
	}

}
