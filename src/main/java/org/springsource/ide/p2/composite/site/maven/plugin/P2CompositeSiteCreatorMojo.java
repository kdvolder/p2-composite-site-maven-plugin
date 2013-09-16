/*******************************************************************************
 *  Copyright (c) 2013 GoPivotal, Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.p2.composite.site.maven.plugin;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * A simple mojo that generates a composite update site from a list
 * of urls of other update sites.
 */
@Mojo( 
		name = "generate", 
		defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
		requiresProject = true
)
public class P2CompositeSiteCreatorMojo extends AbstractMojo
{
	
	@Parameter(required=true)
	String[] sites;
	
	@Parameter(required=false)
	String target;

	@Parameter(required=true)
	String name;
	
	@Component
	MavenProject project;
	
	
	private String timeStamp = ""+System.currentTimeMillis();
	
	private Document createDocument() throws ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		return docBuilder.newDocument();
	}
	
    public void execute() throws MojoExecutionException {
    	try {
	    	if (target==null) {
	    		target = project.getBuild().getDirectory() + "/site";
	    	}
	    	
	    	File targetDir = new File(target);
	    	if (!targetDir.isDirectory()) {
	    		boolean ok = targetDir.mkdirs();
	    		if (!ok) {
	    			throw new Exception("Couldn't create directory: "+targetDir);
	    		}
	    	}
	    	
	    	Document doc = generateCompositeArtifactsXML();
	    	write(doc, new File(targetDir, "compositeArtifacts.xml"));
	    	
	    	doc = generateCompositeContentXML();
	    	write(doc, new File(targetDir, "compositeContent.xml"));
	        
	    	for (String url : sites) {
	            getLog().info(url);
			}
    	} catch (Exception e) {
    		throw new MojoExecutionException("Problem executing: "+this, e);
    	}
    }

	private void write(Document doc, File file) throws TransformerException, IOException {
		
		if (file.exists()) {
			boolean ok = file.delete();
			if (!ok) {
				throw new IOException("File exists and couldn't be deleted: "+file);
			}
		}
		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount","2");		
//		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "5");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(file);
 
		// Output to console for testing
		// StreamResult result = new StreamResult(System.out);
 
		transformer.transform(source, result);
 
		getLog().debug("XML saved: "+file);
	}
	
	private Document generateCompositeContentXML() throws ParserConfigurationException {
		return generateCompositeXML("org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository");
	}
	
	private Document generateCompositeArtifactsXML() throws ParserConfigurationException {
		return generateCompositeXML("org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository");
	}

	private Document generateCompositeXML(String repoType) throws ParserConfigurationException {
		Document doc = createDocument();
		
		Element repository = doc.createElement("repository");
		
		doc.appendChild(repository);
		repository.setAttribute("name", name);
		repository.setAttribute("type", repoType);
		repository.setAttribute("version", "1.0.0");

		
		Element properties = doc.createElement("properties");
		repository.appendChild(properties);
		properties.setAttribute("size", "1");
		
		Element property = doc.createElement("property");
		properties.appendChild(property);
		property.setAttribute("name", "p2.timestamp");
		property.setAttribute("value", timeStamp);

		Element children = doc.createElement("children");
		repository.appendChild(children);
		children.setAttribute("size", ""+sites.length);
		
		for (String siteStr : sites) {
			Element child = doc.createElement("child");
			children.appendChild(child);
			child.setAttribute("location", siteStr);
		}
		
		return doc;
	}
}