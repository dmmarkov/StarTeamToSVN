/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package starteamtosvn;

//import com.starbase.starteam;
import com.starbase.starteam.File;
import com.starbase.starteam.Folder;
import com.starbase.starteam.Item;
import com.starbase.starteam.Project;
import com.starbase.starteam.Server;
import com.starbase.starteam.Type;
import com.starbase.starteam.User;
import com.starbase.starteam.View;
import com.starbase.starteam.vts.comm.NetMonitor;
import com.starbase.util.OLEDate;
import java.io.BufferedReader;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author markov
 */
public class StarTeamToSVN {
    
    private static void ExtractFullTreeFromRoot(com.starbase.starteam.Folder SourceFolder, String SourceFolderName, String RootFolder) {
        ExtractFilesFromFolder(SourceFolder, SourceFolderName, RootFolder);
        
        Item[] RootFolders = SourceFolder.getItems("Folder");
        
        for (Item CurrentItem : RootFolders) {
            Folder CurrentFolder = (Folder)CurrentItem;            
            ExtractFullTreeFromRoot(CurrentFolder, SourceFolderName+CurrentFolder.getPathFragment()+"/", RootFolder);
        }
    }

    private static void ExtractFilesFromFolder(com.starbase.starteam.Folder SourceFolder, String SourceFolderName, String RootFolder) {
        Item[] RootFiles = SourceFolder.getItems("File");
   
        for (Item CurrentItem : RootFiles) {
            com.starbase.starteam.File MyFile = (File) CurrentItem;
            if (SourceFolderName.isEmpty()) {
                ExtractFileHistory(MyFile, "/", RootFolder);
            } else {
                ExtractFileHistory(MyFile, SourceFolderName, RootFolder);
            }
        }
    }
    
    private static String FormatOLEDATEToString(OLEDate SourceValue) {
        DateFormat formatter = new SimpleDateFormat("yyyy.MM.dd.HH.mm");
        return formatter.format(SourceValue.createDate());
    }
    
    private static String FindAuthorNameById(int ID) {
        
        String AuthorName;
        
        switch (ID) {
            case 0:  AuthorName = "StarTeam Server Administrator 1";
                     break;
            case 1:  AuthorName = "StarTeam Server Administrator 2";
                     break;
            case 2:  AuthorName = "StarTeam Server Administrator 3";
                     break;
            case 3:  AuthorName = "StarTeam Server Administrator 4";
                     break;
            case 4:  AuthorName = "StarTeam Server Administrator 5";
                     break;
            default: AuthorName = "There is no such user!!!";
                     break;
        }

        return AuthorName;
    }
    
    private static String FindAuthorUserNameByID(int ID){
        String AuthorName;

        switch (ID) {
            case 0:
                AuthorName = "StarTeamServerAdministrator1";
                break;
            case 1:
                AuthorName = "StarTeamServerAdministrator2";
                break;
            case 2:
                AuthorName = "StarTeamServerAdministrator3";
                break;
            case 3:
                AuthorName = "StarTeamServerAdministrator4";
                break;
            case 4:
                AuthorName = "StarTeamServerAdministrator5";
                break;
            default:
                AuthorName = "There is no such user!!!";
                break;
        }

        return AuthorName;    
    }
    
    private static String FindAuthorPasswordNameByID(int ID){
        
        String AuthorPassword;
        switch (ID) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                AuthorPassword = "12345";
                break;
            default:
                AuthorPassword = "There is no such user!!!";
                break;
        }
        return AuthorPassword;
    }
    
    private static void ExtractFileHistory(com.starbase.starteam.File SourceFile, String SourceFolderName, String RootFolder) {
        Item[] FileHistory = SourceFile.getHistory();
        
        for (Item CurrentHistoryItem : FileHistory) {
            com.starbase.starteam.File CurrentHistoryFile = (File) CurrentHistoryItem;
            
            String FullFileName    = RootFolder + "/" + FormatOLEDATEToString(CurrentHistoryFile.getModifiedTime()) + "/Files" + SourceFolderName + CurrentHistoryFile.getName();
            String FullPath        = RootFolder + "/" + FormatOLEDATEToString(CurrentHistoryFile.getModifiedTime()) + "/Files" + SourceFolderName;
            String HistoryFileName = RootFolder + "/" + FormatOLEDATEToString(CurrentHistoryFile.getModifiedTime()) +"/@History.txt";
            
            System.out.format("FileName = %s; Revision = %d; CreatedTime = %s; Author = %s; Comment = '%s';%n",
                    FullFileName, CurrentHistoryFile.getRevisionNumber() + 1,
                    FormatOLEDATEToString(CurrentHistoryFile.getModifiedTime()),
                    FindAuthorNameById(CurrentHistoryFile.getModifiedBy()), CurrentHistoryFile.getComment());
            
            FileOutputStream fop = null;
	    java.io.File file;
            
            try {
                java.io.File directory = new java.io.File(FullPath);
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                file = new java.io.File(FullFileName);
                fop = new FileOutputStream(file);
                
                CurrentHistoryFile.checkoutToStream(fop, com.starbase.starteam.Item.LockType.UNCHANGED, false);

                fop.flush();
                fop.close();
                
                java.io.File HistoryFile = new java.io.File(HistoryFileName);
                if (!HistoryFile.exists()) {
                    PrintWriter out = new PrintWriter(HistoryFileName);
                    out.println("AuthorID: " + CurrentHistoryFile.getModifiedBy());
                    out.println("AuthorName: " + FindAuthorNameById(CurrentHistoryFile.getModifiedBy()));
                    out.println("TimeStamp: " + FormatOLEDATEToString(CurrentHistoryFile.getModifiedTime()));
                    out.println("Comment: " + CurrentHistoryFile.getComment());
                    out.close();
                }

                //System.out.println("Done");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fop != null) {
                        fop.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private static void PrintUserList(Server CurrentServer) {
        User[] Users = CurrentServer.getUsers();
        for (User CurrentUser : Users) {
            System.out.println(CurrentUser.getID()+" "+CurrentUser.getName()); 
        }  
    }
    
    private static void PrintListOfTypes(Server CurrentServer) {
        Type[] StarTeamTypes = CurrentServer.getTypes();
        for (Type ItemType : StarTeamTypes) {
            System.out.println("ItemType = " + ItemType.getName());
        }       
    }
    
    private static void PrintListOfProjects(Server CurrentServer) {
        Project[] projects = CurrentServer.getProjects();
        for (Project currentproject : projects) {
            System.out.println("ProjectName = "+currentproject.getName());
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {     
        Server StarTeamServer = new Server("WINAPPSRV", 49201);
        StarTeamServer.connect();
        
        if (StarTeamServer.isConnected()) {
            System.out.println("Connect to server OK!");
            StarTeamServer.logOn("markov", "123456"); 
            
            if (StarTeamServer.isLoggedOn()) {
                System.out.println("LogOn to server OK!"); 
               
                Project[] projects = StarTeamServer.getProjects();
                Project TW = null;
                for (Project currentproject : projects) {
                    //System.out.println("ProjectName = "+currentproject.getName());
                    
                    if (currentproject.getName().equals("Tw")) {
                        TW = currentproject;
                        break;
                    }
                } 
                
                if (TW != null) {
                    System.out.println("Try to find first revision");
                    
                    View CurrentView =  TW.getDefaultView();

                    //Путь до точки назначения должен быть с / в конце
                    ExtractFullTreeFromRoot(CurrentView.getRootFolder(), "/", "C:/StarTeamToSVN");
                } else {
                    System.out.println("Project Tw not found in StarTeam repository");
                }
            } else {
                System.out.println("LogOn to server failed :'(");
            }
            
            StarTeamServer.disconnect();
            
            //1. Получить список корневых директорий-ревизий
            java.io.File dir = new java.io.File("C:/StarTeamToSVN");

            java.io.File[] subDirs = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(java.io.File pathname) {
                    return pathname.isDirectory();
                }
            });
            
            //2. Отсортировать список
            Arrays.sort(subDirs);
            
            //3. Пройти по списку директорий и перебросить их содержимое
                //1. Получить список файлов с полными путями, которые собираюсь копировать
                //2. Создать список файлов которых нет в конечной папке SVN
                //3. Содержимое папки Files скопировать с заменой со всеми подпапками из StarTeam в SVN
                //4. Все файлы которые не добавлены в SVN добавить
                //5. Закоммитить ревизию с указанием автора и комментариями
            
            java.io.File RootSVN = new java.io.File("C:\\TestASU");
            
            for (java.io.File CurrentDir : subDirs){
                try {
                    ArrayList<java.io.File> MyFiles = new ArrayList<>();
                    
                    String StarTeamSourceFolder = CurrentDir.getAbsolutePath()+"\\Files\\";
                    listf(StarTeamSourceFolder, MyFiles);
                    
                    ///String[] SVNAddFiles = new String[]();
                    ArrayList<String> SVNAddFiles = new ArrayList<>();
                    
                    for (java.io.File CurrentFile: MyFiles) {
                        String FullSourcePath = CurrentFile.getAbsolutePath();
                        String FullDestPath = "C:\\TestASU\\" + FullSourcePath.substring(FullSourcePath.indexOf(StarTeamSourceFolder) + StarTeamSourceFolder.length()) ;
                        
                        //сначала проверям все папки, а потом проверяем файлы
                        
                        java.io.File DestFile = new java.io.File(FullDestPath);
                        java.io.File ParentFolder = DestFile.getParentFile();
                        while ((ParentFolder != null) && (ParentFolder.compareTo(RootSVN) != 0 )) {
                            if (!ParentFolder.exists()) {
                                SVNAddFiles.add(0, ParentFolder.getAbsolutePath());
                            }
                            ParentFolder = ParentFolder.getParentFile();
                        }

                        if (!DestFile.exists()) {
                            SVNAddFiles.add(FullDestPath);
                        }
                    }
                    
                    //здесь сохранена версия файлов без всяких повторов
                    Set<String> s = new LinkedHashSet<>(SVNAddFiles);
                    
                    //копирование файлов
                    java.io.File RootStarTeam = new java.io.File(StarTeamSourceFolder);
                    try {
                        copyFolder(RootStarTeam, RootSVN);
                    } catch (IOException ex) {
                        Logger.getLogger(StarTeamToSVN.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    Thread.sleep(5000);
                    
                    //добавить отсуствующие папки/файлы
                    for (String NewItem: s) {
                        //SVNCleanUp("C:\\TestASU", CurrentDir.getName());
                        //Thread.sleep(10000);
                        SVNAddFile(NewItem, CurrentDir.getName());                 
                    }

                    //закоммитить с параметрами пользователями
                    String HistoryPath = CurrentDir.getAbsolutePath() + "\\@History.txt";
                    String AuthorUserName = "";
                    String AuthorPassword = "";
                    String AuthorComment = "";
                    try {
                        for (String line : Files.readAllLines(Paths.get(HistoryPath), Charset.defaultCharset())) {
                            if (line.contains("AuthorID")) {
                                String AuthorID = line.substring(line.indexOf(": ")+2);
                                
                                AuthorUserName = FindAuthorUserNameByID(Integer.parseInt(AuthorID));
                                AuthorPassword = FindAuthorPasswordNameByID(Integer.parseInt(AuthorID));
                            }
                            
                            if (line.contains("TimeStamp")) {
                                AuthorComment = line.substring(line.indexOf(": ")+2);
                            }
                            
                            if (line.contains("Comment")) {
                                AuthorComment = AuthorComment + "\n" + line.substring(line.indexOf(": ")+2);
                            }
                            
                            if (!line.contains(": ")) {
                                AuthorComment = AuthorComment + "\n" + line;
                            }
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(StarTeamToSVN.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    if ((AuthorUserName == "")||(AuthorPassword == "")) {
                        throw new IOException("Ho Authentification data");
                    }
                    
                    Thread.sleep(1000);
                    
                    SVNCommit("C:\\TestASU\\", CurrentDir.getName(), AuthorUserName, AuthorPassword, AuthorComment);
                    
                    //Thread.sleep(30000);
                    
                } catch (InterruptedException ex) {
                    Logger.getLogger(StarTeamToSVN.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            System.out.println("Connect to server failed :'(");
        } 
    }
    
    private static void SVNCommit(String Path, String SourceDir, String UserName, String Password, String Comment) {
        try {
            ProcessBuilder procbuilder = new ProcessBuilder();
            if (Comment.contains("\"")) {
                Comment = Comment.replace("\"", "\\\"");
            }
            procbuilder.command("C:\\Program Files\\TortoiseSVN\\bin\\svn.exe", "commit", "--no-auth-cache", "--username", UserName, "--password", Password, "--message", "\"" + Comment + "\"", Path);
            procbuilder.redirectOutput(new java.io.File("C:\\svn_out\\"+SourceDir+"_svn_commit.txt"));
            procbuilder.redirectError(new java.io.File("C:\\svn_out\\" + SourceDir + "_svn_commit_error.txt"));
            Process p = procbuilder.start();
            try {
                int ExitCode = p.waitFor();
                System.out.format("svn commit. exitcode = %d%n", ExitCode);
                if (ExitCode == 1) {
                    System.out.println("I'm here");
                }
                p.destroy();
            } catch (InterruptedException ex) {
                Logger.getLogger(StarTeamToSVN.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IOException ex) {
            Logger.getLogger(StarTeamToSVN.class.getName()).log(Level.SEVERE, null, ex);
        }      
    }
    
    private static void SVNCleanUp(String Path, String SourceDir) {
        try {
            ProcessBuilder procbuilder = new ProcessBuilder();
            procbuilder.command("C:\\Program Files\\TortoiseSVN\\bin\\svn.exe", "cleanup", Path);
            procbuilder.directory(new java.io.File("C:\\Program Files\\TortoiseSVN\\bin"));
            procbuilder.redirectOutput(new java.io.File("C:\\svn_out\\" + SourceDir + "_svn_cleanup.txt"));
            procbuilder.redirectError(new java.io.File("C:\\svn_out\\" + SourceDir + "_svn_cleanup_error.txt"));
            Process p = procbuilder.start();
            try {
                int ExitCode = p.waitFor();
                System.out.format("svn cleanup. exitcode = %d%n", ExitCode);
                if (ExitCode == 1) {
                    System.out.println("I'm here");
                }
                p.destroy();
            } catch (InterruptedException ex) {
                Logger.getLogger(StarTeamToSVN.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        } catch (IOException ex) {
            Logger.getLogger(StarTeamToSVN.class.getName()).log(Level.SEVERE, null, ex);
        }           
    }
    
    private static void SVNAddFile(String Path, String SourceDir) {
        try {
            ProcessBuilder procbuilder = new ProcessBuilder();
            if (Path.contains("@")) {
                Path += "@";
            }
            procbuilder.command("C:\\Program Files\\TortoiseSVN\\bin\\svn.exe", "add", "--depth=empty", "--no-auth-cache", Path);
            procbuilder.directory(new java.io.File("C:\\Program Files\\TortoiseSVN\\bin"));
            procbuilder.redirectOutput(new java.io.File("C:\\svn_out\\" + SourceDir + "_svn_add.txt"));
            procbuilder.redirectError(new java.io.File("C:\\svn_out\\" + SourceDir + "_svn_add_error.txt"));
            Process p = procbuilder.start();
            try {
                int ExitCode = p.waitFor();
                System.out.format("svn add. exitcode = %d%n", ExitCode);
                if (ExitCode == 1) {
                    System.out.println("I'm here");
                }
                p.destroy();
            } catch (InterruptedException ex) {
                Logger.getLogger(StarTeamToSVN.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IOException ex) {
            Logger.getLogger(StarTeamToSVN.class.getName()).log(Level.SEVERE, null, ex);
        }     
    }
    
    private static void copyFolder(java.io.File src, java.io.File dest) throws IOException {
        if (src.isDirectory()) {
            //if directory not exists, create it
            if (!dest.exists()) {
                dest.mkdirs();
            }
            //list all the directory contents
            String files[] = src.list();
            for (String file : files) {
                //construct the src and dest file structure
                java.io.File srcFile = new java.io.File(src, file);
                java.io.File destFile = new java.io.File(dest, file);
                //recursive copy
                copyFolder(srcFile, destFile);
            }
        } else {
    //if file, then copy it
            //Use bytes stream to support all file types
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            //copy the file content in bytes 
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            in.close();
            out.close();
            //System.out.println("File copied from " + src + " to " + dest);
        }
    }
    
    private static void listf(String directoryName, ArrayList<java.io.File> files) {
        java.io.File directory = new java.io.File(directoryName);

        // get all the files from a directory
        java.io.File[] fList = directory.listFiles();
        for (java.io.File file : fList) {
            if (file.isFile()) {
                files.add(file);
            } else if (file.isDirectory()) {
                listf(file.getAbsolutePath(), files);
            }
        }
    }
}
