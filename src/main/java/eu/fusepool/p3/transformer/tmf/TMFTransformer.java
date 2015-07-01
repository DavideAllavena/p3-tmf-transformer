package eu.fusepool.p3.transformer.tmf;

import eu.fusepool.p3.transformer.HttpRequestEntity;
import eu.fusepool.p3.transformer.SyncTransformer;
import eu.fusepool.p3.transformer.commons.Entity;
import eu.fusepool.p3.transformer.commons.util.WritingEntity;
import eu.fusepool.p3.transformer.server.TransformerServer;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.BasicConfigurator;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;



public class TMFTransformer implements SyncTransformer {


    private static enum Transformer {
        sync, async
    }

    private static final Logger fLogger = LoggerFactory.getLogger(TMFTransformer.class);

    public static final String PAR_SCRIPT = "script";

    private static final MimeType MIME_TEXT_PLAIN = mimeType("text", "plain");

    @Option(name = "-p", aliases = {"--port"}, usage = "set the port to which to bind to", metaVar = "7100", required = false)
    private final int fPort = 7101;

    @Option(name = "-t", aliases = {"--type"}, usage = "specify transformer type (sync, async), default: sync", metaVar = "sync", required = false)
    private final Transformer fType = Transformer.sync;

    @SuppressWarnings("serial")
    private static final Set<MimeType> INPUT_FORMATS = Collections
            .unmodifiableSet(new HashSet<MimeType>() {{
                add(MIME_TEXT_PLAIN);
            }});

    private static final Set<MimeType> OUTPUT_FORMATS = INPUT_FORMATS;


    @Override
    public Set<MimeType> getSupportedInputFormats() {
        return INPUT_FORMATS;
    }


    @Override
    public Set<MimeType> getSupportedOutputFormats() {
        return OUTPUT_FORMATS;
    }


    @Override
    public Entity transform(HttpRequestEntity entity) throws IOException {
        String original = IOUtils.toString(entity.getData());

        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost("http://tellmefirst.polito.it:2222/rest/classify");
        httpPost.setHeader("User-Agent", " ");
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        BasicConfigurator.configure();


        // Request parameters and other properties.
        builder.addTextBody("text", original, ContentType.TEXT_PLAIN);
        builder.addTextBody("numTopics", "7", ContentType.TEXT_PLAIN);
        builder.addTextBody("lang", "english", ContentType.TEXT_PLAIN);
        HttpEntity multipart = builder.build();

        httpPost.setEntity(multipart);

        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity responseEntity = response.getEntity();

        String transformed = new String();
        if (responseEntity != null) {
            InputStream instream = responseEntity.getContent();
            try {
                transformed = IOUtils.toString(instream, "UTF-8");
                fLogger.info("Tell me first was asked for this text: " + original);
            } finally {
            }
        }

        return wrapInEntity(transformed);
    }

    private WritingEntity wrapInEntity(final String transformed) {
        return new WritingEntity() {
            @Override
            public MimeType getType() {
                return MIME_TEXT_PLAIN;
            }

            @Override
            public void writeData(OutputStream out) throws IOException {
                out.write(transformed.getBytes());
            }
        };
    }

    @Override
    public boolean isLongRunning() {
        return false;
    }


    private String charsetOf(HttpServletRequest request) {

        String encoding = request.getCharacterEncoding();

        if (encoding == null) {
            fLogger.error("Cannot resolve encoding " + encoding + ". Defaulting to US-ASCII.");
            encoding = "US-ASCII";
        }

        return encoding;
    }


    private static MimeType mimeType(String primary, String sub) {

        try {
            return new MimeType(primary, sub);
        } catch (MimeTypeParseException ex) {
            throw new RuntimeException("Internal error.");
        }
    }

    public void _main(String[] args) throws Exception {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            parser.printUsage(System.out);
            System.exit(-1);
        }

        TransformerServer server = new TransformerServer(fPort, false);
        if (fType.equals(Transformer.sync))
            server.start(new TMFTransformer());
        else
            server.start(new AsyncTMFTransformer());

        try {
            server.join();
        } catch (InterruptedException ex) {
            fLogger.error("Internal error: ", ex);
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws Exception {
        new TMFTransformer()._main(args);
    }

}
