package com.lrm.web.admin;

import com.google.common.io.ByteStreams;
import com.lrm.po.Blog;
import com.lrm.po.EditormdUploadImageResModel;
import com.lrm.po.User;
import com.lrm.service.BlogService;
import com.lrm.service.TagService;
import com.lrm.service.TypeService;
import com.lrm.vo.BlogQuery;
import org.apache.tomcat.util.http.fileupload.FileItemFactory;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by limi on 2017/10/15.
 */
@Controller
@RequestMapping("/admin")
public class BlogController {

    private static final String INPUT = "admin/blogs-input";
    private static final String LIST = "admin/blogs";
    private static final String REDIRECT_LIST = "redirect:/admin/blogs";


    @Autowired
    private BlogService blogService;
    @Autowired
    private TypeService typeService;
    @Autowired
    private TagService tagService;

    @GetMapping("/blogs")
    public String blogs(@PageableDefault(size = 8, sort = {"updateTime"}, direction = Sort.Direction.DESC) Pageable pageable,
                        BlogQuery blog, Model model) {
        model.addAttribute("types", typeService.listType());
        model.addAttribute("page", blogService.listBlog(pageable, blog));
        return LIST;
    }

    @PostMapping("/blogs/search")
    public String search(@PageableDefault(size = 8, sort = {"updateTime"}, direction = Sort.Direction.DESC) Pageable pageable,
                         BlogQuery blog, Model model) {
        model.addAttribute("page", blogService.listBlog(pageable, blog));
        return "admin/blogs :: blogList";
    }


    @GetMapping("/blogs/input")
    public String input(Model model) {
        setTypeAndTag(model);
        model.addAttribute("blog", new Blog());
        return INPUT;
    }

    private void setTypeAndTag(Model model) {
        model.addAttribute("types", typeService.listType());
        model.addAttribute("tags", tagService.listTag());
    }


    @GetMapping("/blogs/{id}/input")
    public String editInput(@PathVariable Long id, Model model) {
        setTypeAndTag(model);
        Blog blog = blogService.getBlog(id);
        blog.init();
        model.addAttribute("blog", blog);
        return INPUT;
    }


    @PostMapping("/blogs")
    public String post(Blog blog, RedirectAttributes attributes, HttpSession session) {
        blog.setUser((User) session.getAttribute("user"));
        blog.setType(typeService.getType(blog.getType().getId()));
        blog.setTags(tagService.listTag(blog.getTagIds()));
        Blog b;
        if (blog.getId() == null) {
            b = blogService.saveBlog(blog);
        } else {
            b = blogService.updateBlog(blog.getId(), blog);
        }

        if (b == null) {
            attributes.addFlashAttribute("message", "操作失败");
        } else {
            attributes.addFlashAttribute("message", "操作成功");
        }
        return REDIRECT_LIST;
    }


    @GetMapping("/blogs/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes attributes) {
        blogService.deleteBlog(id);
        attributes.addFlashAttribute("message", "删除成功");
        return REDIRECT_LIST;
    }

     @RequestMapping(value = "/editormd/uploadImage"/*, method = RequestMethod.POST*/)
    @ResponseBody
    //@PostMapping("/editormd/uploadImage")
    public EditormdUploadImageResModel editormduploadImage(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        EditormdUploadImageResModel res = new EditormdUploadImageResModel();
        res.setSuccess(0);
        ServletContext application = request.getSession().getServletContext();
        String savePath = application.getRealPath("/") + "images22/";
        User user = (User) session.getAttribute("user");
        //String savePath ="d:/niuhao-images/"+user.getUsername()+"/images/";
        // 文件保存目录URL
        String saveUrl = request.getContextPath() + "/images22/";
        // 定义允许上传的文件扩展名
        HashMap<String, String> extMap = new HashMap<String, String>();
        String dirName = user.getUsername();
        extMap.put(dirName, "gif,jpg,jpeg,png,bmp");
        extMap.put("flash", "swf,flv");
        extMap.put("media", "swf,flv,mp3,wav,wma,wmv,mid,avi,mpg,asf,rm,rmvb");
        extMap.put("file", "doc,docx,xls,xlsx,ppt,htm,html,txt,zip,rar,gz,bz2");

        // 最大文件大小
        long maxSize = 3000000;

        response.setContentType("text/html; charset=UTF-8");

        if (!ServletFileUpload.isMultipartContent(request)) {
            return res;

        }
        // 检查目录
        File uploadDir = new File(savePath);
        if (!uploadDir.isDirectory()) {
            uploadDir.mkdirs();
            // return getError("上传目录不存在。");
        }
        // 检查目录写权限
        if (!uploadDir.canWrite()) {
            return res;

        }

        //String dirName = request.getParameter("dir");

        if (dirName == null) {
            dirName = "image";
        }
        if (!extMap.containsKey(dirName)) {
            return res;

        }
        // 创建文件夹
        savePath += dirName + "/";
        saveUrl += dirName + "/";
        File saveDirFile = new File(savePath);
        if (!saveDirFile.exists()) {
            saveDirFile.mkdirs();
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String ymd = sdf.format(new Date());
        savePath += ymd + "/";
        saveUrl += ymd + "/";
        File dirFile = new File(savePath);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }

        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setHeaderEncoding("UTF-8");


        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;

        Iterator item = multipartRequest.getFileNames();
        while (item.hasNext()) {

            String fileName = (String) item.next();

            MultipartFile file = multipartRequest.getFile(fileName);


            // 检查文件大小

            if (file.getSize() > maxSize) {

                return res;


            }

            // 检查扩展名

            String fileExt = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1).toLowerCase();

            if (!Arrays.asList(extMap.get(dirName).split(",")).contains(fileExt)) {
                return res;

            }
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");

            String newFileName = df.format(new Date()) + "_" + new Random().nextInt(1000) + "." + fileExt;

            try {

                File uploadedFile = new File(savePath, newFileName);

                //保存
                ByteStreams.copy(file.getInputStream(), new FileOutputStream(uploadedFile));

                //file.transferTo(uploadedFile);
            } catch (Exception e) {
                return res;
            }
            res.setSuccess(1);
            res.setMessage(fileName);
            res.setUrl(saveUrl + newFileName);
            return res;
        }
        return res;
    }


}
