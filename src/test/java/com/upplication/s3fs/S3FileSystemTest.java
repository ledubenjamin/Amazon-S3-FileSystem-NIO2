package com.upplication.s3fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.upplication.s3fs.util.AmazonS3ClientMock;
import com.upplication.s3fs.util.AmazonS3MockFactory;

public class S3FileSystemTest extends S3UnitTest {
	private FileSystem fs;

	@Before
	public void setup() throws IOException {
		AmazonS3ClientMock client = AmazonS3MockFactory.getAmazonClientMock();
		client.addBucket("bucketA");
		client.addBucket("bucketB");
		fs = FileSystems.getFileSystem(S3_GLOBAL_URI);
	}

	@Test
	public void getPathFirst() {
		assertEquals(fs.getPath("/bucket"), fs.getPath("/bucket"));
		assertEquals(fs.getPath("file"), fs.getPath("file"));
	}

	@Test
	public void getPathFirstWithMultiplesPaths() {
		assertEquals(fs.getPath("/bucket/path/to/file"), fs.getPath("/bucket/path/to/file"));
		assertNotEquals(fs.getPath("/bucket/path/other/file"), fs.getPath("/bucket/path/to/file"));

		assertEquals(fs.getPath("dir/path/to/file"), fs.getPath("dir/path/to/file"));
		assertNotEquals(fs.getPath("dir/path/other/file"), fs.getPath("dir/path/to/file"));
	}

	@Test
	public void getPathFirstAndMore() {
		Path actualAbsolute = fs.getPath("/bucket", "dir", "file");
		assertEquals(fs.getPath("/bucket", "dir", "file"), actualAbsolute);
		assertEquals(fs.getPath("/bucket/dir/file"), actualAbsolute);

		Path actualRelative = fs.getPath("dir", "dir", "file");
		assertEquals(fs.getPath("dir", "dir", "file"), actualRelative);
		assertEquals(fs.getPath("dir/dir/file"), actualRelative);
	}

	@Test
	public void getPathFirstAndMoreWithMultiplesPaths() {
		Path actual = fs.getPath("/bucket", "dir/file");
		assertEquals(fs.getPath("/bucket", "dir/file"), actual);
		assertEquals(fs.getPath("/bucket/dir/file"), actual);
		assertEquals(fs.getPath("/bucket", "dir", "file"), actual);
	}

	@Test
	public void getPathFirstWithMultiplesPathsAndMoreWithMultiplesPaths() {
		Path actual = fs.getPath("/bucket/dir", "dir/file");
		assertEquals(fs.getPath("/bucket/dir", "dir/file"), actual);
		assertEquals(fs.getPath("/bucket/dir/dir/file"), actual);
		assertEquals(fs.getPath("/bucket", "dir", "dir", "file"), actual);
		assertEquals(fs.getPath("/bucket/dir/dir", "file"), actual);
	}

	@Test
	public void getPathRelativeAndAbsoulte() {
		assertNotEquals(fs.getPath("/bucket"), fs.getPath("bucket"));
		assertNotEquals(fs.getPath("/bucket/dir"), fs.getPath("bucket/dir"));
		assertNotEquals(fs.getPath("/bucket", "dir"), fs.getPath("bucket", "dir"));
		assertNotEquals(fs.getPath("/bucket/dir", "dir"), fs.getPath("bucket/dir", "dir"));
		assertNotEquals(fs.getPath("/bucket", "dir/file"), fs.getPath("bucket", "dir/file"));
		assertNotEquals(fs.getPath("/bucket/dir", "dir/file"), fs.getPath("bucket/dir", "dir/file"));
	}

	@Test
	public void duplicatedSlashesAreDeleted() {
		Path actualFirst = fs.getPath("/bucket//file");
		assertEquals(fs.getPath("/bucket/file"), actualFirst);
		assertEquals(fs.getPath("/bucket", "file"), actualFirst);

		Path actualFirstAndMore = fs.getPath("/bucket//dir", "dir//file");
		assertEquals(fs.getPath("/bucket/dir/dir/file"), actualFirstAndMore);
		assertEquals(fs.getPath("/bucket", "dir/dir/file"), actualFirstAndMore);
		assertEquals(fs.getPath("/bucket/dir", "dir/file"), actualFirstAndMore);
		assertEquals(fs.getPath("/bucket/dir/dir", "file"), actualFirstAndMore);
	}

	@Test
	public void readOnlyAlwaysFalse() {
		assertTrue(!fs.isReadOnly());
	}

	@Test
	public void getSeparatorSlash() {
		assertEquals("/", fs.getSeparator());
		assertEquals("/", S3Path.PATH_SEPARATOR);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void getPathMatcherThrowException() {
		fs.getPathMatcher("");
	}

	@Test(expected = UnsupportedOperationException.class)
	public void getUserPrincipalLookupServiceThrowException() {
		fs.getUserPrincipalLookupService();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void newWatchServiceThrowException() throws Exception {
		fs.newWatchService();
	}

	@Test(expected = IllegalArgumentException.class)
	public void getPathWithoutBucket() {
		fs.getPath("//path/to/file");
	}

	@Test
	public void getFileStores() {
		Iterable<FileStore> result = fs.getFileStores();
		assertNotNull(result);
		Iterator<FileStore> iterator = result.iterator();
		assertNotNull(iterator);
		assertTrue(iterator.hasNext());
		assertNotNull(iterator.next());
	}

	@Test
	public void getRootDirectoriesReturnBuckets() {

		Iterable<Path> paths = fs.getRootDirectories();

		assertNotNull(paths);

		int size = 0;
		boolean bucketNameA = false;
		boolean bucketNameB = false;

		for (Path path : paths) {
			String name = path.getFileName().toString();
			if (name.equals("bucketA")) {
				bucketNameA = true;
			} else if (name.equals("bucketB")) {
				bucketNameB = true;
			}
			size++;
		}

		assertEquals(2, size);
		assertTrue(bucketNameA);
		assertTrue(bucketNameB);
	}

	@Test
	public void supportedFileAttributeViewsReturnBasic() {
		Set<String> operations = fs.supportedFileAttributeViews();

		assertNotNull(operations);
		assertTrue(!operations.isEmpty());

		for (String operation : operations) {
			assertEquals("basic", operation);
		}
	}

	@Test
	public void getRootDirectories() {
		fs.getRootDirectories();
	}

	@Test
	public void close() throws IOException {
		assertTrue(fs.isOpen());
		fs.close();
		assertTrue(!fs.isOpen());
	}

	private static void assertNotEquals(Object a, Object b) {
		assertTrue(a + " are not equal to: " + b, !a.equals(b));
	}
	
	@Test
	public void comparables() throws IOException {
		S3FileSystemProvider provider = new S3FileSystemProvider();
		S3FileSystem s3fs1 = provider.getFileSystem(URI.create("s3://mirror1.amazon.test/"));
		S3FileSystem s3fs2 = provider.getFileSystem(URI.create("s3://mirror2.amazon.test/"));
		S3FileSystem s3fs3 = provider.getFileSystem(URI.create("s3://accessKey:secretKey@mirror1.amazon.test/"));
		S3FileSystem s3fs4 = provider.getFileSystem(URI.create("s3://accessKey:secretKey@mirror2.amazon.test"));
		S3FileSystem s3fs5 = provider.getFileSystem(URI.create("s3://mirror1.amazon.test/"));
		S3FileSystem s3fs6 = provider.getFileSystem(URI.create("s3://access_key:secret_key@mirror1.amazon.test/"));
		AmazonS3ClientMock amazonClientMock = AmazonS3MockFactory.getAmazonClientMock();
		S3FileSystem s3fs7 = new S3FileSystem(provider, null, amazonClientMock, "mirror1.amazon.test", CHARSET);
		S3FileSystem s3fs8 = new S3FileSystem(provider, null, amazonClientMock, null, CHARSET);
		S3FileSystem s3fs9 = new S3FileSystem(provider, null, amazonClientMock, null, CHARSET);
		S3FileSystem s3fs10 = new S3FileSystem(provider, "somekey", amazonClientMock, null, CHARSET);
		S3FileSystem s3fs11 = new S3FileSystem(provider, "access key for test@mirror2.amazon.test", amazonClientMock, "mirror2.amazon.test", CHARSET);
		
		assertEquals(-517310489, s3fs1.hashCode());
		assertEquals(-1316272121, s3fs2.hashCode());
		assertEquals(-636290468, s3fs3.hashCode());
		assertEquals(-1435252100, s3fs4.hashCode());
		assertEquals(-517310489, s3fs5.hashCode());
		assertEquals(-1866959227, s3fs6.hashCode());
		assertEquals(-82123487, s3fs7.hashCode());
		
		assertFalse(s3fs1.equals(s3fs2));
		assertFalse(s3fs1.equals(s3fs3));
		assertFalse(s3fs1.equals(s3fs4));
		assertTrue(s3fs1.equals(s3fs5));
		assertFalse(s3fs1.equals(s3fs6));
		assertFalse(s3fs3.equals(s3fs4));
		assertFalse(s3fs3.equals(s3fs6));
		assertFalse(s3fs1.equals(s3fs6));
		assertFalse(s3fs1.equals(new S3FileStore(s3fs1, "emmer")));
		assertFalse(s3fs7.equals(s3fs8));
		assertTrue(s3fs8.equals(s3fs8));
		assertFalse(s3fs8.equals(s3fs1));
		assertTrue(s3fs8.equals(s3fs9));
		assertFalse(s3fs9.equals(s3fs10));
		assertTrue(s3fs2.equals(s3fs11));

		assertEquals(0, s3fs1.compareTo(s3fs5));
		assertEquals(-1, s3fs1.compareTo(s3fs2));
		assertEquals(1, s3fs2.compareTo(s3fs1));
		assertEquals(-63, s3fs1.compareTo(s3fs6));
		s3fs7.close();
		s3fs8.close();
		s3fs9.close();
		s3fs10.close();
		s3fs11.close();
	}
	
	@Test(expected=UnsupportedEncodingException.class)
	public void unknownCharsetKey2Parts() throws Throwable {
		S3FileSystemProvider provider = new S3FileSystemProvider();
		AmazonS3ClientMock amazonClientMock = AmazonS3MockFactory.getAmazonClientMock();
		S3FileSystem s3fs = new S3FileSystem(provider, null, amazonClientMock, "mirror1.amazon.test", "unknown");
		try {
			s3fs.key2Parts("/bucket/folder%20with%20spaces/file");
		} catch(RuntimeException e) {
			throw e.getCause();
		} finally {
			try {
				s3fs.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}
	
	@Test
	public void key2Parts() {
		S3FileSystemProvider provider = new S3FileSystemProvider();
		AmazonS3ClientMock amazonClientMock = AmazonS3MockFactory.getAmazonClientMock();
		S3FileSystem s3fs = new S3FileSystem(provider, null, amazonClientMock, "mirror1.amazon.test", "UTF-8");
		try {
			String[] parts = s3fs.key2Parts("/bucket/folder%20with%20spaces/file");
			assertEquals("", parts[0]);
			assertEquals("bucket", parts[1]);
			assertEquals("folder with spaces", parts[2]);
			assertEquals("file", parts[3]);
		} finally {
			try {
				s3fs.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}
	
	@Test
	public void parts2Key() {
		S3FileSystemProvider provider = new S3FileSystemProvider();
		AmazonS3ClientMock amazonClientMock = AmazonS3MockFactory.getAmazonClientMock();
		S3FileSystem s3fs = new S3FileSystem(provider, null, amazonClientMock, "mirror1.amazon.test", "unknown");
		try {
			assertEquals("/bucket/folder%20with%20spaces/file", s3fs.parts2Key(Arrays.asList("/bucket", "folder with spaces", "file")));
		} finally {
			try {
				s3fs.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}
}