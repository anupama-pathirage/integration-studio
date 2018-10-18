/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.

 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at

 *      http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.developerstudio.eclipse.carbonserver44microei.operations;

import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerPort;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.wso2.developerstudio.eclipse.carbon.server.model.util.CarbonServerCommonConstants;
import org.wso2.developerstudio.eclipse.carbon.server.model.util.CarbonServerCommonUtils;
import org.wso2.developerstudio.eclipse.carbonserver.base.exception.CarbonServerNotRunningException;
import org.wso2.developerstudio.eclipse.carbonserver.base.exception.NoSuchCarbonOperationDefinedException;
import org.wso2.developerstudio.eclipse.carbonserver.base.impl.CarbonServer;
import org.wso2.developerstudio.eclipse.carbonserver.base.interfaces.ICarbonServerMonitor;
import org.wso2.developerstudio.eclipse.carbonserver.base.manager.CarbonServerManager;
import org.wso2.developerstudio.eclipse.carbonserver.base.manager.ICarbonOperationManager;
import org.wso2.developerstudio.eclipse.carbonserver44microei.Activator;
import org.wso2.developerstudio.eclipse.carbonserver44microei.monitor.CarbonServerListener;
import org.wso2.developerstudio.eclipse.carbonserver44microei.operations.ServiceModuleOperations;
import org.wso2.developerstudio.eclipse.carbonserver44microei.util.CarbonServer44eiUtils;
import org.wso2.developerstudio.eclipse.logging.core.IDeveloperStudioLog;
import org.wso2.developerstudio.eclipse.logging.core.Logger;
import org.wso2.developerstudio.eclipse.server.base.core.ServerController;
import org.wso2.developerstudio.eclipse.utils.file.FileUtils;
import org.xml.sax.SAXException;
import org.wso2.developerstudio.eclipse.carbonserver.base.utils.CarbonServerUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class CarbonOperationsManager44ei implements ICarbonOperationManager {

	private static final String runtimeId = "org.wso2.developerstudio.eclipse.carbon.runtime44ei";
	private static IDeveloperStudioLog log = Logger.getLog(Activator.PLUGIN_ID);
	private CarbonServer44eiUtils carbonServer44eiUtils = new CarbonServer44eiUtils();
	private CommonOperations commonOperations = new CommonOperations();

	public String getRuntimeId() {
		return runtimeId;
	}

	public Object executeOperation(Map<String, Object> operation) throws Exception {
		if (operation.containsKey(ICarbonOperationManager.PARAMETER_TYPE)) {
			try {
				int opType = Integer.parseInt(operation.get(ICarbonOperationManager.PARAMETER_TYPE).toString());
				Object result = null;
				IServer server = null;
				if (operation.containsKey(ICarbonOperationManager.PARAMETER_SERVER))
					server = (IServer) operation.get(ICarbonOperationManager.PARAMETER_SERVER);
				switch (opType) {
					case ICarbonOperationManager.OPERATION_SUPPORTED_OPERATIONS:
						result = false;
						if (operation.containsKey(ICarbonOperationManager.PARAMETER_OP_TYPES))
							result =
							         isOperationSupported(Integer.parseInt(operation.get(ICarbonOperationManager.PARAMETER_OP_TYPES)
							                                                        .toString()));
						break;
					case ICarbonOperationManager.OPERATION_GET_LIBRARY_PATHS:
						result = getLibraryPaths().toArray(new String[] {});
						break;
					case ICarbonOperationManager.OPERATION_GET_CODEGEN_LIBRARIES:
						if (server == null) {
							if (operation.containsKey(ICarbonOperationManager.PARAMETER_RUNTIME)) {
								String path = (String) operation.get(ICarbonOperationManager.PARAMETER_PATH);
								List<String> libraryPaths = getLibraryPaths();
								List<String> absLibraryPaths = new ArrayList<String>();
								for (String string : libraryPaths) {
									absLibraryPaths.add(FileUtils.addAnotherNodeToPath(path, string));
								}
								result = getCodegenLibraries(absLibraryPaths.toArray(new String[] {}));
							}
						} else {
							String[] serverCodegenLibraries = CarbonServerManager.getServerLibraryPaths(server);
							result = getCodegenLibraries(serverCodegenLibraries);
						}
						break;
					case ICarbonOperationManager.OPERATION_GET_AXIS2_LIBRARIES:
						if (operation.containsKey(ICarbonOperationManager.PARAMETER_PATH)) {
							String path = operation.get(ICarbonOperationManager.PARAMETER_PATH).toString();
							result = getAxis2Libraries(path);

						}
						break;
					case ICarbonOperationManager.OPERATION_GET_SERVER_HOME:
						if (server != null)
							result = commonOperations.getWSASHome(server);
						break;
					case ICarbonOperationManager.OPERATION_GET_SERVER_CONF:
						if (server != null)
							result = getServerConfDirPath(server);
						break;
					case ICarbonOperationManager.OPERATION_GET_SERVER_PORTS:
						if (server != null)
							result = carbonServer44eiUtils.getServerPorts(server);
						break;
					case ICarbonOperationManager.OPERATION_INITIALIZE_SERVER_CONFIGURATIONS:
						if (server != null)
							initializeTheServer(server);
						break;
					case ICarbonOperationManager.OPERATION_CLEANUP_SERVER_CONFIGURATIONS:
						if (server != null)
							cleanupTheServer(server);
						break;
					case ICarbonOperationManager.OPERATION_PUBLISH_MODULE:
						if (server != null)
							if (operation.containsKey(ICarbonOperationManager.PARAMETER_PROJECT)) {
								IProject project = (IProject) operation.get(ICarbonOperationManager.PARAMETER_PROJECT);
								ServiceModuleOperations serviceModuleOperations =
								                                                  new ServiceModuleOperations(project,
								                                                                              server);
								String path = null;
								serviceModuleOperations.publishServiceModule(path, null);
							}
						break;
					case ICarbonOperationManager.OPERATION_UNPUBLISH_MODULE:
						if (server != null)
							if (operation.containsKey(ICarbonOperationManager.PARAMETER_PROJECT)) {
								IProject project = (IProject) operation.get(ICarbonOperationManager.PARAMETER_PROJECT);
								ServiceModuleOperations serviceModuleOperations =
								                                                  new ServiceModuleOperations(project,
								                                                                              server);
								String path = null;
								serviceModuleOperations.unpublishServiceModule(path, null);
							}
						break;
					case ICarbonOperationManager.OPERATION_HOT_UPDATE_MODULE:
						if (server != null)
							if (operation.containsKey(ICarbonOperationManager.PARAMETER_PROJECT)) {
								IProject project = (IProject) operation.get(ICarbonOperationManager.PARAMETER_PROJECT);
								ServiceModuleOperations serviceModuleOperations =
								                                                  new ServiceModuleOperations(project,
								                                                                              server);
								IResource updatedResource = (IResource) operation.get("resource");
								serviceModuleOperations.setUpdatedResource(updatedResource);
								Integer resChangeKind = (Integer) operation.get("resourceChangeKind");
								if (resChangeKind != null) {
									serviceModuleOperations.setResourceChngeKind(resChangeKind);
								}
								serviceModuleOperations.hotUpdateModule();
							}
						break;
					case ICarbonOperationManager.OPERATION_REDEPLOY_MODULE:
						if (server != null)
							if (operation.containsKey(ICarbonOperationManager.PARAMETER_PROJECT)) {
								IProject project = (IProject) operation.get(ICarbonOperationManager.PARAMETER_PROJECT);
								ServiceModuleOperations serviceModuleOperations =
								                                                  new ServiceModuleOperations(project,
								                                                                              server);
								serviceModuleOperations.redeployModule();
							}
						break;
					case ICarbonOperationManager.OPERATION_GET_PUBLISHED_SERVICES:
						if (server != null) {
							ServiceModuleOperations serviceModuleOperations;
							if (operation.containsKey(ICarbonOperationManager.PARAMETER_PROJECT)) {
								IProject project = (IProject) operation.get(ICarbonOperationManager.PARAMETER_PROJECT);
								serviceModuleOperations = new ServiceModuleOperations(project, server);
							} else {
								serviceModuleOperations = new ServiceModuleOperations(null, server);
							}
							return serviceModuleOperations.getServicesList();
						}
						break;
					case ICarbonOperationManager.OPERATION_GET_SERVICE_WSDL_URL:
						URL serviceWSDLUrl = null;
						if (server != null) {
							if (operation.containsKey(ICarbonOperationManager.PARAMETER_SERVICE_NAME)) {
								String serviceName =
								                     (String) operation.get(ICarbonOperationManager.PARAMETER_SERVICE_NAME);
								serviceWSDLUrl = getServiceWSDLUrl(server, serviceName);
							}
						}
						result = serviceWSDLUrl;
						break;
					case ICarbonOperationManager.OPERATION_GET_SERVICE_TRY_IT_URL:
						URL serviceTryItUrl = null;
						if (server != null) {
							if (operation.containsKey(ICarbonOperationManager.PARAMETER_SERVICE_NAME)) {
								String serviceName =
								                     (String) operation.get(ICarbonOperationManager.PARAMETER_SERVICE_NAME);
								serviceTryItUrl = getServiceTryItUrl(server, serviceName);
							}
						}
						result = serviceTryItUrl;
						break;
					case ICarbonOperationManager.OPERATION_GET_SERVER_CREDENTIALS:
						Map<String, String> serverCredentials = null;
						if (server != null) {
							serverCredentials = CarbonServerCommonUtils.getServerCredentials(server);
						}
						result = serverCredentials;
						break;
					case ICarbonOperationManager.OPERATION_GET_SERVER_AUTHENTICATED_COOKIE:
						String cookie = null;
						if (server != null) {
							if (operation.containsKey(ICarbonOperationManager.PARAMETER_SERVER_PORT)) {
								String serverPort =
								                    (String) operation.get(ICarbonOperationManager.PARAMETER_SERVER_PORT);
								cookie = CarbonServerCommonUtils.getServerCookie(server, serverPort);
							}
						}
						result = cookie;
						break;
					case ICarbonOperationManager.OPERATION_SERVER_URL:
						result = carbonServer44eiUtils.getServerURL(server);
						break;
					case ICarbonOperationManager.OPERATION_SERVER_VERSION:
						result = carbonServer44eiUtils.getServerVersion();
						break;

					default:
						throw new NoSuchCarbonOperationDefinedException();
				}

				return result;
			} catch (Exception ex) {
				throw ex;
			}
		} else {
			throw new NoSuchCarbonOperationDefinedException();
		}
	}

	public boolean isOperationSupported(int operation) {
		boolean result = false;
		switch (operation) {
			case OPERATION_SUPPORTED_OPERATIONS:
			case OPERATION_GET_LIBRARY_PATHS:
			case OPERATION_GET_SERVER_HOME:
			case OPERATION_GET_SERVER_PORTS:
			case OPERATION_WSDL_CONVERT_FILE:
			case OPERATION_WSDL_CONVERT_URL:
			case OPERATION_WSDL_TRY_IT_URL:
			case OPERATION_WSDL_TRY_IT_FILE:
			case OPERATION_GET_CODEGEN_LIBRARIES:
			case OPERATION_GET_AXIS2_LIBRARIES:
			case OPERATION_GET_SERVICE_WSDL_URL:
			case OPERATION_GET_SERVICE_TRY_IT_URL:
			case OPERATION_REDEPLOY_MODULE:
				result = true;
				break;
			default:
				result = false;
		}
		return result;
	}

	public void initializeTheServer(IServer server) throws CoreException {
		String serverLocalWorkspacePath = CarbonServerManager.getServerLocalWorkspacePath(server);
		String serverLocalRepoLocation = carbonServer44eiUtils.getRepositoryPathFromLocalWorkspaceRepo(serverLocalWorkspacePath);
		String serverLocalConfLocation = carbonServer44eiUtils.getConfPathFromLocalWorkspaceRepo(serverLocalWorkspacePath);
		carbonServer44eiUtils.updateTransportPorts(server);
		carbonServer44eiUtils.updateAxis2XML(server);
		// just retrieving the value for hotupdate will update the axis2.xml
		// also
		isServerHotUpdate(server);

		initializeServerRepoLibraries(server);
		List<ICarbonServerMonitor> serverMonitors = CarbonServer.getServerMonitors(server);
		IServerListener serverListener = new CarbonServerListener(serverMonitors);
		server.addServerListener(serverListener);
	}

	public void cleanupTheServer(IServer server) {
		unpublishAllModules(server);
	}

	private void unpublishAllModules(IServer server) {
		IModule[] modules = server.getModules();
		for (IModule module : modules) {
			ServiceModuleOperations serviceModuleOperation = new ServiceModuleOperations(module.getProject(), server);
			serviceModuleOperation.unpublishServiceModule(null, null);
		}
	}

	private String[] getCodegenLibraries(String[] serverCodegenLibraries) throws Exception {
		List<String> paths = new ArrayList<String>();
		List<String> alreadyAdded = new ArrayList<String>();
		for (String lib : serverCodegenLibraries) {
			visitAllFiles(new File(lib), paths, alreadyAdded);
		}
		return paths.toArray(new String[] {});
	}

	private String[] getAxis2Libraries(String wsasHome) throws Exception {
		initializeServerRepoLibraries(wsasHome);
		IPath home = new Path(wsasHome);
		IPath libFolder = home.append("repository").append("lib");
		String[] list = (new File(libFolder.toOSString())).list();
		for (int i = 0; i < list.length; i++) {
			list[i] = libFolder.append(list[i]).toOSString();
		}
		return list;
	}

	private void visitAllFiles(File dir, List paths, List alreadyAdded) {
		if (!dir.toString().endsWith(".txt")) {
			if (dir.isDirectory()) {
				if (dir.getName().toString().equals("tomcat") || dir.getName().toString().equals("wsf") ||
				    dir.getName().toString().equals("patches")) {
					// skip these libraris being loaded
				} else {
					String[] children = dir.list();
					for (int i = 0; i < children.length; i++) {
						visitAllFiles(new File(dir, children[i]), paths, alreadyAdded);
					}
				}
			} else {
				if (isValidFile(dir.getName(), alreadyAdded))
					paths.add(dir.getAbsolutePath());
			}

		}
	}

	private boolean isValidFile(String fileName, List alreadyAdded) {
		if (fileName.toLowerCase().startsWith("org.wso2.carbon")) {
			for (String prefix : CarbonServerCommonConstants.getStringArrayFromProprties(Activator.PLUGIN_ID, "AXIS2_CODEGEN_LIB_PREFIXES")) {
				if (!alreadyAdded.contains(prefix))
					if (fileName.toLowerCase().contains(prefix.toLowerCase())) {
						alreadyAdded.add(prefix);
						return true;
					}
			}
		} else {
			for (String prefix : CarbonServerCommonConstants.getStringArrayFromProprties(Activator.PLUGIN_ID, "AXIS2_UTIL_LIB_PREFIXES")) {
				if (!alreadyAdded.contains(prefix))
					if (fileName.toLowerCase().contains(prefix.toLowerCase())) {
						alreadyAdded.add(prefix);
						return true;
					}
			}
		}
		return false;

	}

	private void initializeServerRepoLibraries(IServer server) throws CoreException {
		IPath wsasHome = commonOperations.getWSASHome(server);
		initializeServerRepoLibraries(wsasHome.toOSString());
	}

	private void initializeServerRepoLibraries(String wsasHomeFodler) throws CoreException {
		IPath wsasHome = new Path(wsasHomeFodler);
		if (!(new File(wsasHome.append("repository").append("lib").toOSString())).exists() ||
		    (new File(wsasHome.append("repository").append("lib").toOSString())).list().length < 5) {
			AntRunner runner = new AntRunner();
			IPath antFile = wsasHome.append("bin").append("build.xml");
			if (antFile.toFile().exists()) {
				runner.setBuildFileLocation(antFile.toOSString());
				//fixing the server not starting (osgi) issue
				//below line executes the build file and copies the jars from repo/components/plugins to lib
				//which is not needed for server to operate
				//runner.run(new NullProgressMonitor());
			}
		}
	}

	private List<String> getLibraryPaths() {
		String libPath1 = FileUtils.addNodesToPath("", new String[] { "lib" });
		String libPath2 = FileUtils.addNodesToPath("", new String[] { "repository", "lib" });
		String libPath3 = FileUtils.addNodesToPath("", new String[] { "webapps", "ROOT", "WEB-INF", "plugins" });
		List<String> paths = new ArrayList<String>();
		paths.add(libPath1);
		paths.add(libPath2);
		paths.add(libPath3);
		return paths;
	}

	private URL getServiceWSDLUrl(IServer server, String serviceName) {
		int httpPort = 0;
		ServerPort[] serverPorts = ServerController.getInstance().getServerManager().getServerPorts(server);
		for (ServerPort serverPort : serverPorts) {
			if (serverPort.getProtocol().equalsIgnoreCase("http"))
				httpPort = serverPort.getPort();
		}
		String serviceWSDLUrl =
		                        "http://localhost:" + httpPort + "/" + CarbonServerUtils.getServicePath() + "/" +
		                                serviceName + "?wsdl";
		URL url = null;
		try {
			url = new URL(serviceWSDLUrl);
		} catch (MalformedURLException e) {
			log.error(e);
		}
		return url;
	}

	private URL getServiceTryItUrl(IServer server, String serviceName) {
		int httpPort = 0;
		ServerPort[] serverPorts = ServerController.getInstance().getServerManager().getServerPorts(server);
		for (ServerPort serverPort : serverPorts) {
			if (serverPort.getProtocol().equalsIgnoreCase("http"))
				httpPort = serverPort.getPort();
		}
		String serviceWSDLUrl =
		                        "http://localhost:" + httpPort + "/" + CarbonServerUtils.getServicePath() + "/" +
		                                serviceName + "?tryit";
		URL url = null;
		try {
			url = new URL(serviceWSDLUrl);
		} catch (MalformedURLException e) {
			log.error(e);
		}
		return url;
	}
	
	private static Boolean isServerHotUpdate(IServer server) {
		String value = CarbonServerCommonUtils.getServerConfigMapValue(server, "carbon.hotupdate");
		if (value == null) {
			return null;
		}
		boolean enabled = value.toString().equalsIgnoreCase("true");
		if (enabled != isHotUpdateEnabled(server)) {
			setHotUpdateEnabled(server, enabled);
		}
		return enabled;
	}

	private static boolean setHotUpdateEnabled(IServer server, boolean enabled) {
		if (isHotUpdateEnabled(server) == enabled)
			return true;
		String axis2Xml = getAxis2FilePath(server);
		XPathFactory factory = XPathFactory.newInstance();
		try {
			File xmlDocument = new File(axis2Xml);
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = builder.parse(xmlDocument);
			XPath xPath = factory.newXPath();
			Node httpNode =
			                (Node) xPath.evaluate("/axisconfig/parameter[@name='hotupdate']", document,
			                                      XPathConstants.NODE);
			httpNode.setTextContent(enabled ? "true" : "false");
			Transformer t = TransformerFactory.newInstance().newTransformer();
			File confPath = new File((new File(axis2Xml)).getParent());
			if (!confPath.exists())
				confPath.mkdirs();
			Result result = new StreamResult(new File(axis2Xml));
			Source source = new DOMSource(document);
			t.transform(source, result);
			return true;
		} catch (FileNotFoundException e) {
			log.error(e);
		} catch (XPathExpressionException e) {
			log.error(e);
		} catch (ParserConfigurationException e) {
			log.error(e);
		} catch (SAXException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		} catch (TransformerConfigurationException e) {
			log.error(e);
		} catch (TransformerFactoryConfigurationError e) {
			log.error(e);
		} catch (TransformerException e) {
			log.error(e);
		}
		return false;
	}
	
	private static boolean isHotUpdateEnabled(IServer server) {
		String axis2Xml = getAxis2FilePath(server);
		XPathFactory factory = XPathFactory.newInstance();
		try {
			File xmlDocument = new File(axis2Xml);
			if (xmlDocument.exists()) {
				DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document document = builder.parse(xmlDocument);
				XPath xPath = factory.newXPath();
				Node httpNode =
				                (Node) xPath.evaluate("/axisconfig/parameter[@name='hotupdate']", document,
				                                      XPathConstants.NODE);
				return httpNode.getTextContent().toString().equalsIgnoreCase("true");
			} else {
				return false;
			}
		} catch (FileNotFoundException e) {
			log.error(e);
		} catch (XPathExpressionException e) {
			log.error(e);
		} catch (ParserConfigurationException e) {
			log.error(e);
		} catch (SAXException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		} catch (TransformerFactoryConfigurationError e) {
			log.error(e);
		}
		return false;
	}
	
	public static String getAxis2FilePath(IServer server) {
		IPath serverHome = CarbonServerManager.getServerHome(server);
		String axis2Xml =
		                  FileUtils.addNodesToPath(serverHome.toOSString(), new String[] { "conf",
		                                                                                  "axis2", "axis2.xml" });
		return axis2Xml;
	}
	
	public IPath getServerConfDirPath(IServer server) {
		IPath serverHome = commonOperations.getWSASHome(server);
		String conf = FileUtils.addNodesToPath(serverHome.toOSString(), new String[] { "conf"});
		return new Path(conf);
	}
}
