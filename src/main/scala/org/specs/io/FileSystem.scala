package org.specs.io;
import java.io._
import java.util.regex._
import org.specs.collection.JavaConversions
import scala.collection.mutable.Queue
import org.specs.log.Log
import java.net.URL
import java.util.zip._

/**
 * The fs object offers a simple interface to the file system (see the description of the FileSystem trait)
 */
object fs extends FileSystem

/**
 * The FileSystem trait abstracts file system operations to allow easier mocking of file system related functionalities.
 * <p>
 * It mixes the <code>FileReader</code> and <code>FileWriter</code> traits to provide easy read/write operations.  
 */
trait FileSystem extends FileReader with FileWriter with JavaConversions {
  object logger extends Log with ConsoleOutput
  /**
   * @param path glob expression, for example: <code>./dir/**/*.xml</code>
   * @return the list of paths represented by the "glob" definition <code>path</path>  
   */
  def filePaths(path: String): List[String] = {
    var pattern = globToPattern(path)
    if (isDir(path))
      pattern += "/*.*"
    val result = new Queue[String]
    collectFiles(result, new File("."), pattern)
    result.toList
  }
  
  /**
   * @param result list of collected file paths
   * @param file current file being examined
   * @param pattern regular expression which should be matching the file path
   */
  private def collectFiles(result: Queue[String], file: File, pattern: String): Unit = {
    logger.debug("try to accept " + file.getPath.replace("\\", "/") + " with " + pattern)
    if (file.isFile && file.getPath.replace("\\", "/").matches(pattern)) {
      logger.debug("got a match")
      result += (file.getPath)
    }
    else if (file.listFiles != null) {
      file.listFiles.foreach { collectFiles(result, _, pattern) }
    }
  }
  
  /**
   * @return the regular expression equivalent to a glob pattern (see the specs for examples)
   */
  def globToPattern(glob: String): String = {
    val star = "<STAR>"
    val authorizedNamePattern = "[^\\/\\?<>\\|\\" + star + ":\"]" + star
    var pattern = glob.replace("\\", "/")
    									.replace(".", "\\.")
    									.replace("**/", "(" + authorizedNamePattern + "/)" + star)
                      .replace("*", authorizedNamePattern)
                      .replace(star, "*")
    if (!pattern.startsWith("\\./"))
      pattern = "\\./" + pattern 
    pattern
  }
  
  /**
   * @return true if the File represented by this path is a directory
   */
  def isDir(path: String) = isDirectory(path)

  /**
   * creates a directory for a given path
   */
  def createDir(path: String) = (new File(path)).mkdirs

  /**
   * creates a file for a given path. Create the parent directory if necessary
   */
  def createFile(path: String) = {
    if (!new File(path).getParentFile.exists) createDir(new File(path).getParent) 
    new File(path).createNewFile
  }

  /**
   * deletes the directory and all directory contents at the specified path and return the parent path of that directory
   */
  def removeDir(path: String): String = {
    val dir = new File(path)
    if (dir.isDirectory) { 
      if (dir.listFiles == null || dir.listFiles.isEmpty)
        dir.delete
      else {
        dir.listFiles.foreach { file => 
          if (file.isFile) 
            file.delete
          else 
            removeDir(file.getPath)
        }
        dir.delete
      }
    }
    dir.getParent
  }
  /** @return true if the file exists */
  def exists(path: String) = path != null && new File(path).exists  

  /** @return true if the file can be read */
  def canRead(path: String) = path != null && new File(path).canRead  

  /** @return true if the file can be written */
  def canWrite(path: String) = path != null && new File(path).canWrite  

  /** @return true if the file is absolute */
  def isAbsolute(path: String) = path != null && new File(path).isAbsolute  

  /** @return true if the file is a file */
  def isFile(path: String) = path != null && new File(path).isFile  

  /** @return true if the file is a directory */
  def isDirectory(path: String) = path != null && new File(path).isDirectory  

  /** @return true if the file is hidden */
  def isHidden(path: String) = path != null && new File(path).isHidden  

  /** @return the file name */
  def getName(path: String) = new File(path).getName  

  /** @return the file absolute path */
  def getAbsolutePath(path: String) = new File(path).getAbsolutePath  

  /** @return the file canonical path */
  def getCanonicalPath(path: String) = new File(path).getCanonicalPath  

  /** @return the file parent path */
  def getParent(path: String) = new File(path).getParent  

  /** @return the files of that directory */
  def listFiles(path: String): List[String] = if (new File(path).list == null) List() else new File(path).list.toList
  
  /** 
   * copy the content of a directory to another.
   * @param url url of the directory to copy
   * @param dest destination directory path
   */
  def copyDir(url: URL, dest: String): Unit = copyDir(new File(url.toURI).getPath, dest)
  /** 
   * copy the content of a directory to another.
   * @param path path of the directory to copy
   * @param dest destination directory path
   */
  def copyDir(src: String, dest: String): Unit = listFiles(src).foreach { name => 
    copyFile(src + "/" + name, dest) 
  }
  /** 
   * Copy the content of a directory to another.
   * @param path path of the file to copy
   * @param dest destination directory path
   */
  def copyFile(path: String, dest: String) = {
    mkdirs(dest)
    val destFilePath = dest + "/" + new File(path).getName
    new File(destFilePath).createNewFile
    val input = new BufferedInputStream(new FileInputStream(path))
    val output  = new BufferedOutputStream(new FileOutputStream(destFilePath), 2048)
    copy(input, output)
    output.flush
    output.close
    input.close
  }  
  /** 
   * Unjar the jar (or zip file) specified by "path" to the "dest" directory.
   * @param path path of the jar file
   * @param dest destination directory path
   */
  def unjar(path: String, dest: String): Unit = unjar(path, dest, ".*")
  
  /** 
   * Unjar the jar (or zip file) specified by "path" to the "dest" directory.
   * Filters files which shouldn't be extracted with a regular expression.
   * @param path path of the jar file
   * @param dest destination directory path
   * @param regexFilter regular expression filtering files which shouldn't be extracted
   */
  def unjar(path: String, dirPath: String, regexFilter: String) = {
	 mkdirs(dirPath)
     val fis = new FileInputStream(path)
     val zis = new ZipInputStream(new BufferedInputStream(fis))
     var entry: ZipEntry = null
     def extractEntry(entry: ZipEntry): Unit = {
       if (entry != null) {
         if (entry.getName.matches(regexFilter)) {
		     if (entry.isDirectory()){
		       createDir(dirPath + "/" + entry.getName)
		     } else {
		       createFile(dirPath + "/" + entry.getName)
		       val fos = new FileOutputStream(dirPath + "/" + entry.getName)
		       val dest = new BufferedOutputStream(fos, 2048)
		       copy(zis, dest)
               dest.flush
		       dest.close
		     }
           
         }
         extractEntry(zis.getNextEntry)
       }
     } 
     extractEntry(zis.getNextEntry)
     zis.close
  }
  
  /** 
   * Copy an input stream to an output stream.
   * @param input input stream
   * @param output output stream
   */
  def copy(input: InputStream, output: OutputStream) = {
    val data = new Array[Byte](2048)
    def readData(count: Int): Unit = {
      if (count != -1) {
        output.write(data, 0, count)  
        readData(input.read(data, 0, 2048))
      }
    }
    readData(input.read(data, 0, 2048))
  }

  /** 
   * Copy specs resources found either in the specs jar or in the classpath directories to an output directory.
   * Current limitations!! This only works if the jar holding the resources contains the word "specs".
   * @param src name of the resource directory to copy
   * @param outputDir output directory where to copy the files to
   */
  def copySpecResourcesDir(src: String, outputDir: String) = {
    val dirUrls = ClassLoader.getSystemResource(src) :: this.getClass.getClassLoader.getResources(src) 
    Set(dirUrls.toList:_*) foreach { dirUrl => 
      if (dirUrl.toString.startsWith("jar")) {
        if (dirUrl.toString.toLowerCase.contains("specs")) 
          unjar(dirUrl.toString.replace("jar:file:/", "").takeWhile(_ != '!').mkString, outputDir, ".*" + src + "/.*")
      } else {
         copyDir(dirUrl, outputDir + src)
      }
      
    } 
  }
  

}
