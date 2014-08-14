package org.odk.collect.android.io;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.IOCipherFileChannel;
import info.guardianproject.iocipher.VirtualFileSystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.javarosa.core.io.BufferedInputStream;

import android.util.Log;

/**
 * Wrapper around a secure file storage system. This implementation uses
 * IOCipher https://guardianproject.info/code/iocipher/
 * 
 * @author davidbrodsky
 * 
 */
public class SecureFileStorageManager {
	private static final String TAG = "SecureFileStorageManager";

	private String mVirtualFilesystemPath;
	private VirtualFileSystem mVfs;

	/**
	 * Create a new secure virtual file system hosted within the provided file.
	 */
	public SecureFileStorageManager(String virtualFilesystemPath) {
		mVirtualFilesystemPath = virtualFilesystemPath;
	}

	/**
	 * Mount the secure virtual file system using the given decryption key
	 */
	public void mountFilesystem(String password) {
		if (mVfs.isMounted()) {
			Log.w(TAG,
					"mountFilesystem called with filesystem all ready mounted. Ignoring.");
			return;
		}
		mVfs = new VirtualFileSystem(mVirtualFilesystemPath);
		mVfs.mount(password);
	}

	/**
	 * Unmount the secure virtual file system.
	 */
	public void unmountFilesystem() {
		if (!mVfs.isMounted()) {
			Log.w(TAG,
					"unmountFilesystem called without filesystem mounted. Ignoring.");
			return;
		}
		mVfs.unmount();
	}

	public boolean isFilesystemMounted() {
		return mVfs.isMounted();
	}

	/**
	 * Write an InputStream to a virtual file within the secure file system.
	 * 
	 * @param fileName
	 * @param is
	 */
	public void writeFile(String fileName, InputStream is) {
		try {
			info.guardianproject.iocipher.FileOutputStream fos = new info.guardianproject.iocipher.FileOutputStream(
					fileName);

			ReadableByteChannel sourceFileChannel = Channels.newChannel(is);
			IOCipherFileChannel destinationFileChannel = fos.getChannel();
			destinationFileChannel.transferFrom(sourceFileChannel, 0,
					is.available());

		} catch (IOException e) {
			Log.w(TAG, "Error writing virtual file " + fileName);
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns a BufferedInputStream corresponding to the data 
	 * stored in the virtual secure filesystem as fileName.
	 * 
	 * @throws FileNotFoundException if fileName does not exist in the virtual
	 * secure filesystem.
	 */
	public BufferedInputStream openFile(String fileName) throws FileNotFoundException {
		return new BufferedInputStream(new FileInputStream(new File(fileName)));
	}

}
