package sh.hella;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.hella.html.util.PreRender;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Mojo(name="render", defaultPhase = LifecyclePhase.PACKAGE)
public class RenderMojo extends AbstractMojo {
    private static final Logger logger = LoggerFactory.getLogger(RenderMojo.class.getName());

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "outputDirectory", defaultValue = "/static/")
    String outputDirectory;

    @Parameter(property = "usePackageSubdirectories", defaultValue = "true")
    String usePackageSubdirectories;

    @Parameter(property = "outputFileSuffix", defaultValue = ".html")
    String outputFileSuffix;

    public void execute() throws MojoExecutionException {
        try {
            for (Class<?> type : reflections().getTypesAnnotatedWith(PreRender.class)) {
                render(type);
            }
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException | DependencyResolutionRequiredException | IOException ex) {
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void render(Class<?> type) throws IOException, NoSuchMethodException,
            InvocationTargetException, InstantiationException, IllegalAccessException {
        boolean usePackageSubdirectories = Boolean.parseBoolean(this.usePackageSubdirectories);
        String outputDirectoryPath = project.getBuild().getDirectory() + outputDirectory
                + (usePackageSubdirectories ? type.getPackageName().replace(".", "/") : "");
        new File(outputDirectoryPath).mkdirs();
        String path = outputDirectoryPath + "/" + type.getSimpleName() + outputFileSuffix;

        logger.info("Rendering {} to {}", type.getName(), path);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            Object object = type.getConstructor().newInstance();
            writer.write(object.toString());
            writer.flush();
        }
    }

    @SuppressWarnings("unchecked")
    private Reflections reflections() throws MojoExecutionException, DependencyResolutionRequiredException {
        List<String> classpathElements = project.getCompileClasspathElements();
        List<URL> projectClasspathList = new ArrayList<>();
        for (String element : classpathElements) {
            try {
                projectClasspathList.add(new File(element).toURI().toURL());
            } catch (MalformedURLException ex) {
                throw new MojoExecutionException(ex);
            }
        }

        return new Reflections(new ConfigurationBuilder().setUrls(projectClasspathList.toArray(new URL[0])));
    }
}
