/**
 * Back 2 Browser Bytecode Translator
 * Copyright (C) 2012-2015 Jaroslav Tulach <jaroslav.tulach@apidesign.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://opensource.org/licenses/GPL-2.0.
 */
package org.apidesign.bck2brwsr.launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import java.util.logging.Logger;
import org.apidesign.bck2brwsr.aot.Bck2BrwsrJars;
import org.apidesign.bck2brwsr.launcher.BaseHTTPLauncher.Res;
import org.apidesign.vm4brwsr.Bck2Brwsr;

/**
 *
 * @author Jaroslav Tulach
 */
class CompileCP {
    private static final Logger LOG = Logger.getLogger(CompileCP.class.getName());
    static String compileJAR(final File jar, Set<String> testClasses) 
    throws IOException {
        StringWriter w = new StringWriter();
        try {
            Bck2BrwsrJars.configureFrom(null, jar)
                .addExported(testClasses.toArray(new String[0]))
                .generate(w);
            w.flush();
            return w.toString();
        } catch (IOException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new IOException("Cannot compile: ", ex);
        } finally {
            w.close();
        }
    }
    
    static String compileFromClassPath(URL u, final Res r) throws IOException {
        File f;
        try {
            f = new File(u.toURI());
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
        String s = f.isDirectory() ? f.getPath() : null;
        
        for (String candidate : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (s != null) {
                break;
            }
            if (f.getPath().startsWith(candidate)) {
                s = candidate;
            }
        }
        if (s != null) {
            File root = new File(s);
            StringWriter w = new StringWriter();
            try {
                Bck2BrwsrJars.configureFrom(null, root)
                    .generate(w);
                w.flush();
                return w.toString();
            } catch (ClassFormatError ex) {
                throw new IOException(ex);
            } finally {
                w.close();
            }
        }
        return null;
    }
    
    static void compileVM(StringBuilder sb, final Res r) throws IOException {
        final Bck2Brwsr rt;
        try {
            URL u = r.get(InterruptedException.class.getName().replace('.', '/') + ".class", 0);
            if (u == null) {
                throw new IOException("Cannot find InterruptedException class on classpath: " + System.getProperty("java.class.path"));
            }
            JarURLConnection juc = (JarURLConnection)u.openConnection();
            rt = Bck2BrwsrJars.configureFrom(null, new File(juc.getJarFileURL().toURI()));
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
        final Bck2Brwsr all;
        try {
            URL u = r.get(Bck2Brwsr.class.getName().replace('.', '/') + ".class", 0);
            JarURLConnection juc = (JarURLConnection)u.openConnection();
            all = Bck2BrwsrJars.configureFrom(rt, new File(juc.getJarFileURL().toURI()));
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }

        all
            .standalone(true)
            //.obfuscation(ObfuscationLevel.FULL)
            .resources(new Bck2Brwsr.Resources() {
                @Override
                public InputStream get(String resource) throws IOException {
                    final URL url = r.get(resource, 0);
                    return url == null ? null : url.openStream();
                }
            }).generate(sb);
    }
}
