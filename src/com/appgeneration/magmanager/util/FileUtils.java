/**
 * 
 */
package com.appgeneration.magmanager.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.appgeneration.magmanager.model.Issue;

/**
 * @author miguelferreira
 * 
 */
public class FileUtils {

	// tag
	public static final String TAG = "FileUtils";
	// local
	public static final String JSON_REL_DIR = "json";
	public static final String JSON_FILE__NAME = "magazine_json.json";

	// document file types
	private static final String FILE_EXT_ZIP = ".zip";
	private static final String FILE_EXT_PDF = ".pdf";

	// remote
	private static final String REMOTE_JSON_BASE_URL = "http://publigeneration.com/magmanager/?q=magmanager/json/";

	public static String getRemoteUrlFromMagazineIdAndApiVersion(
			long magazineId, int jsonVersion) {
		return REMOTE_JSON_BASE_URL + jsonVersion + "/" + magazineId;
	}

	/**
	 * Returns a file object pointing to the local json file where magazine
	 * information shall be stored
	 * 
	 * @param context
	 * @return
	 */
	public static File getLocalMagazineJsonFile(Context context) {
		return new File(context.getDir(FileUtils.JSON_REL_DIR,
				Context.MODE_PRIVATE).getAbsolutePath()
				+ File.separator + FileUtils.JSON_FILE__NAME);
	}

	/**
	 * Returns a file object pointing to the given issue's completed path. This
	 * file may or may not exist
	 * 
	 * @param issue
	 * @param context
	 * @return
	 */
	public static File getIssueFileCompletedPath(Issue issue, Context context) {
		return new File(context.getDir("issues", Context.MODE_PRIVATE)
				.getAbsolutePath()
				+ File.separator
				+ issue.getId()
				+ issue.getFileType());
	}

	/**
	 * Returns a file object pointing to the given issue's unzipped path. This
	 * file may or may not exist but the issue must be a zip file
	 * 
	 * @param issue
	 * @param context
	 * @return
	 */
	public static File getIssueUnzippedPath(Issue issue, Context context) {
		return new File(context.getDir("issues", Context.MODE_PRIVATE)
				.getAbsolutePath() + File.separator + issue.getId());
	}

	/**
	 * Returns a file object pointing to the given issue's partial path. This
	 * file may or may not exist
	 * 
	 * @param issue
	 * @param context
	 * @param fileType
	 *            The issue's filetype starting with a period, ex: ".zip"
	 * @return
	 */
	public static File getIssuePartialPath(Issue issue, Context context) {
		return new File(context.getDir("issues", Context.MODE_PRIVATE)
				.getAbsolutePath()
				+ File.separator
				+ issue.getId()
				+ issue.getFileType() + ".part");
	}

	/**
	 * Decompresses a zip file to its location Adapted from
	 * http://stackoverflow.
	 * com/questions/3382996/how-to-unzip-files-programmatically-in-android
	 * 
	 * @param issueToBeUnzipped
	 * @param context
	 * @return A String representing the newly decompressed file path
	 * @throws IOException
	 */
	public static boolean unzip(String inputZip, String destinationDirectory,
			AsyncTask<?, ?, ?> task) throws IOException {
		Log.i("FileUtils", "Unzipping file..");

		int BUFFER = 2048;
		List zipFiles = new ArrayList();
		File sourceZipFile = new File(inputZip);
		File unzipDestinationDirectory = new File(destinationDirectory);
		unzipDestinationDirectory.mkdir();

		ZipFile zipFile;
		// Open Zip file for reading
		zipFile = new ZipFile(sourceZipFile, ZipFile.OPEN_READ);

		// Create an enumeration of the entries in the zip file
		Enumeration zipFileEntries = zipFile.entries();

		// Process each entry
		while (zipFileEntries.hasMoreElements()) {
			// grab a zip file entry
			ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();

			String currentEntry = entry.getName();

			File destFile = new File(unzipDestinationDirectory, currentEntry);
			// destFile = new File(unzipDestinationDirectory,
			// destFile.getName());

			if (currentEntry.endsWith(".zip")) {
				zipFiles.add(destFile.getAbsolutePath());
			}

			// grab file's parent directory structure
			File destinationParent = destFile.getParentFile();

			// create the parent directory structure if needed
			destinationParent.mkdirs();

			if (task != null) {
				if (task.isCancelled()) {
					return false;
				}
			}

			try {
				// extract file if not a directory
				if (!entry.isDirectory()) {
					BufferedInputStream is = new BufferedInputStream(
							zipFile.getInputStream(entry));
					int currentByte;
					// establish buffer for writing file
					byte data[] = new byte[BUFFER];

					// write the current file to disk
					FileOutputStream fos = new FileOutputStream(destFile);
					BufferedOutputStream dest = new BufferedOutputStream(fos,
							BUFFER);

					// read and write until last byte is encountered
					while ((currentByte = is.read(data, 0, BUFFER)) != -1) {

						if (task != null && task.isCancelled()) {
							dest.flush();
							dest.close();
							is.close();
							return false;
						}

						dest.write(data, 0, currentByte);
					}
					dest.flush();
					dest.close();
					is.close();
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

		try {
			zipFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		for (Iterator iter = zipFiles.iterator(); iter.hasNext();) {
			String zipName = (String) iter.next();
			unzip(zipName,
					destinationDirectory + File.separatorChar
							+ zipName.substring(0, zipName.lastIndexOf(".zip")),
					task);
		}

		Log.i("FileUtils", "Finished unzipping file..");

		return true;
	}

	/**
	 * Checks if an issue's document is a zip
	 * 
	 * @param issueToBheChecked
	 * @return
	 */
	public static boolean isIssueAZipFile(Issue issueToBheChecked) {
		return issueToBheChecked.getFileType().equalsIgnoreCase(FILE_EXT_ZIP);
	}

	/**
	 * Checks if an issue's document is a pdf
	 * 
	 * @param issueToBheChecked
	 * @return
	 */
	public static boolean isIssueAPDFFile(Issue issueToBheChecked) {
		return issueToBheChecked.getFileType().equalsIgnoreCase(FILE_EXT_PDF);
	}

	public static boolean deleteIssueContents(Issue issueToBeDeleted,
			Context context) {
		// just delete the file itself
		File issueFileToBeDeleted = getIssueFileCompletedPath(issueToBeDeleted,
				context);
		if (issueFileToBeDeleted.exists()) {
			if (!issueFileToBeDeleted.delete()) {
				return false;
			}
		}

		// check temporary path (ex: download aborted or paused)
		File issueToBeDeletedTemporary = getIssuePartialPath(
				issueToBeDeleted, context);
		if (issueToBeDeletedTemporary.exists()) {
			if (!issueToBeDeletedTemporary.delete()) {
				return false;
			}
		}

		if (FileUtils.isIssueAZipFile(issueToBeDeleted)) {

			File unzippedIssueToBeDeleted = getIssueUnzippedPath(
					issueToBeDeleted, context);

			if (unzippedIssueToBeDeleted.exists()) {

				delete(unzippedIssueToBeDeleted);
				return true;

			}
		} else {
			throw new AssertionError("Unknow file type "
					+ issueToBeDeleted.getFileType());
		}
		return true;
	}

	public static void delete(File file) {

		if (file.isDirectory()) {

			// directory is empty, then delete it
			if (file.list().length == 0) {

				file.delete();
				System.out.println("Directory is deleted : "
						+ file.getAbsolutePath());

			} else {

				// list all the directory contents
				String files[] = file.list();

				for (String temp : files) {
					// construct the file structure
					File fileDelete = new File(file, temp);

					// recursive delete
					delete(fileDelete);
				}

				// check the directory again, if empty then delete it
				if (file.list().length == 0) {
					file.delete();
					System.out.println("Directory is deleted : "
							+ file.getAbsolutePath());
				}
			}

		} else {
			// if file, then delete it
			file.delete();
			System.out.println("File is deleted : " + file.getAbsolutePath());
		}
	}
}
