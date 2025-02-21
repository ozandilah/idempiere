/******************************************************************************
 * This file is part of Adempiere ERP Bazaar                                  *
 * http://www.adempiere.org                                                   *
 *                                                                            *
 * Copyright (C) Jorg Viola                                                   *
 * Copyright (C) Contributors                                                 *
 *                                                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *                                                                            *
 * Contributors:                                                              *
 * - Heng Sin Low                                                             *
 * - Andreas Sumerauer                                                        *
 *****************************************************************************/
package org.adempiere.webui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.adempiere.base.IServiceReferenceHolder;
import org.adempiere.base.Service;
import org.adempiere.base.ServiceQuery;
import org.adempiere.webui.adwindow.AbstractADWindowContent;
import org.adempiere.webui.adwindow.IADTabpanel;
import org.adempiere.webui.apps.IProcessParameterListener;
import org.adempiere.webui.apps.graph.IChartRendererService;
import org.adempiere.webui.factory.IADTabPanelFactory;
import org.adempiere.webui.factory.IDashboardGadgetFactory;
import org.adempiere.webui.factory.IFindWindowFactory;
import org.adempiere.webui.factory.IFormFactory;
import org.adempiere.webui.factory.IMappedFormFactory;
import org.adempiere.webui.factory.IQuickEntryFactory;
import org.adempiere.webui.grid.AbstractWQuickEntry;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.window.FindWindow;
import org.compiere.grid.ICreateFrom;
import org.compiere.grid.ICreateFromFactory;
import org.compiere.grid.IPaymentForm;
import org.compiere.grid.IPaymentFormFactory;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MDashboardContent;
import org.compiere.util.CCache;
import org.idempiere.ui.zk.media.IMediaView;
import org.idempiere.ui.zk.media.IMediaViewProvider;
import org.idempiere.ui.zk.report.IReportViewerRenderer;
import org.zkoss.zk.ui.Component;

/**
 * Entry point to get implementation instance for UI extensions (through OSGI service or Equinox extension).
 * @author viola
 * @author hengsin
 *
 */
/**
 * 
 */
public class Extensions {

	/** FormId:IFormFactory Reference. FormId is Java class name, OSGi component name or Equinox extension Id */
	private final static CCache<String, IServiceReferenceHolder<IFormFactory>> s_formFactoryCache = new CCache<>(null, "IFormFactory", 100, false);
	
	/**
	 * Get ADForm instance
	 * @param formId Java class name, OSGi component name or equinox extension Id
	 * @return IFormController instance or null if formId not found
	 */
	public static ADForm getForm(String formId) {
		IServiceReferenceHolder<IFormFactory> cache = s_formFactoryCache.get(formId);
		if (cache != null) {
			IFormFactory service = cache.getService();
			if (service != null) {
				ADForm form = service.newFormInstance(formId);
				if (form != null)
					return form;
			}
			s_formFactoryCache.remove(formId);
		}
		List<IServiceReferenceHolder<IFormFactory>> factories = Service.locator().list(IFormFactory.class).getServiceReferences();
		if (factories != null) {
			for(IServiceReferenceHolder<IFormFactory> factory : factories) {
				IFormFactory service = factory.getService();
				if (service != null) {
					ADForm form = service.newFormInstance(formId);
					if (form != null) {
						s_formFactoryCache.put(formId, factory);
						return form;
					}
				}
			}
		}
		return null;
	}
	
	/** CacheKey:IProcessParameterListener Reference. CacheKey is ProcessClassName|ColumnName */
	private final static CCache<String, List<IServiceReferenceHolder<IProcessParameterListener>>> s_processParameterListenerCache = new CCache<>(null, "List<IProcessParameterListener>", 100, false);
	
	/**
	 * Get process parameter listeners
	 * @param processClass Java class name of process
	 * @param columnName
	 * @return list of {@link IProcessParameterListener}
	 */
	public static List<IProcessParameterListener> getProcessParameterListeners(String processClass, String columnName) {
		String cacheKey = processClass + "|" + columnName;
		List<IServiceReferenceHolder<IProcessParameterListener>> listeners = s_processParameterListenerCache.get(cacheKey);
		if (listeners != null)
			return listeners.stream().filter(e -> e.getService() != null).map(e -> e.getService()).collect(Collectors.toCollection(ArrayList::new));
		
		ServiceQuery query = new ServiceQuery();
		query.put("ProcessClass", processClass);
		if (columnName != null)
			query.put("|(ColumnName", columnName+")(ColumnName="+columnName+",*)(ColumnName="+"*,"+columnName+",*)(ColumnName=*,"+columnName+")");
		listeners = Service.locator().list(IProcessParameterListener.class, null, query).getServiceReferences();
		if (listeners == null)
			listeners = new ArrayList<>();
		s_processParameterListenerCache.put(cacheKey, listeners);
		return listeners.stream().filter(e -> e.getService() != null).map(e -> e.getService()).collect(Collectors.toCollection(ArrayList::new));
	}
	
	/** AD_Tab_ID:ICreateFromFactory Reference */
	private final static CCache<String, IServiceReferenceHolder<ICreateFromFactory>> s_createFromFactoryCache = new CCache<>(null, "ICreateFromFactory", 100, false);
	
	/**
	 * Get CreateFrom instance
	 * @param mTab
	 * @return ICreateFrom instance
	 */
	public static ICreateFrom getCreateFrom(GridTab mTab) {
		ICreateFrom createFrom = null;
		String cacheKey = Integer.toString(mTab.getAD_Tab_ID());
		IServiceReferenceHolder<ICreateFromFactory> cache = s_createFromFactoryCache.get(cacheKey);
		if (cache != null) {
			ICreateFromFactory service = cache.getService();
			if (service != null) {
				createFrom = service.create(mTab);
				if (createFrom != null) {
					return createFrom;
				}
			}
			s_createFromFactoryCache.remove(cacheKey);
		}
		
		List<IServiceReferenceHolder<ICreateFromFactory>> factories = Service.locator().list(ICreateFromFactory.class).getServiceReferences();
		for (IServiceReferenceHolder<ICreateFromFactory> factory : factories) 
		{
			ICreateFromFactory service = factory.getService();
			if (service != null) {
				createFrom = service.create(mTab);
				if (createFrom != null) {
					s_createFromFactoryCache.put(cacheKey, factory);
					return createFrom;
				}
			}
		}
		
		return null;
	}
	
	/** CacheKey:IPaymentFormFactory Reference. CacheKey is AD_Tab_ID|Payment Rule */
	private static final CCache<String, IServiceReferenceHolder<IPaymentFormFactory>> s_paymentFormFactoryCache = new CCache<>(null, "IPaymentFormFactory", 100, false);
	
	/**
	 * Get payment form instance
	 * @param windowNo
	 * @param mTab GridTab
	 * @param paymentRule
	 * @return IPaymentForm instance
	 */
	public static IPaymentForm getPaymentForm(int windowNo, GridTab mTab, String paymentRule) {
		String cacheKey = mTab.getAD_Tab_ID() + "|" + paymentRule;
		IServiceReferenceHolder<IPaymentFormFactory> cache = s_paymentFormFactoryCache.get(cacheKey);
		if (cache != null)  {
			IPaymentFormFactory service = cache.getService();
			if (service != null) {
				IPaymentForm paymentForm = service.create(windowNo, mTab, paymentRule);
				if (paymentForm != null)
					return paymentForm;
			}
			s_paymentFormFactoryCache.remove(cacheKey);
		}
		IPaymentForm paymentForm = null;
		List<IServiceReferenceHolder<IPaymentFormFactory>> factories = Service.locator().list(IPaymentFormFactory.class).getServiceReferences();
		for (IServiceReferenceHolder<IPaymentFormFactory> factory : factories) {
			IPaymentFormFactory service = factory.getService();
			if (service != null) {
				paymentForm = service.create(windowNo, mTab, paymentRule);
				if (paymentForm != null) {
					s_paymentFormFactoryCache.put(cacheKey, factory);
					return paymentForm;
				}
			}
		}
		return null;
	}
	
	/** URL:IDashboardGadgetFactory Reference */
	private static final CCache<String, IServiceReferenceHolder<IDashboardGadgetFactory>> s_dashboardGadgetFactoryCache = new CCache<>(null, "IDashboardGadgetFactory", 100, false);
	
	/**
	 * Get dashboard gadget component
	 * @param url
	 * @param parent
	 * @return Gadget component
	 */
	public static final Component getDashboardGadget(String url, Component parent) {
		return getDashboardGadget(url, parent, null);
	}

	/**
	 * Get dashboard gadget component
	 * @param url
	 * @param parent
	 * @param dc
	 * @return Gadget component
	 */
	public static final Component getDashboardGadget(String url, Component parent, MDashboardContent dc) {
		IServiceReferenceHolder<IDashboardGadgetFactory> cache = s_dashboardGadgetFactoryCache.get(url);
		if (cache != null) {
			IDashboardGadgetFactory service = cache.getService();
			if (service != null) {
				Component component = service.getGadget(url,parent,dc);
	            if(component != null)
	            	return component;
			}
			s_dashboardGadgetFactoryCache.remove(url);
		}
		
		List<IServiceReferenceHolder<IDashboardGadgetFactory>> f = Service.locator().list(IDashboardGadgetFactory.class).getServiceReferences();
        for (IServiceReferenceHolder<IDashboardGadgetFactory> factory : f) {
        	IDashboardGadgetFactory service = factory.getService();
        	if (service != null) {
				Component component = service.getGadget(url,parent,dc);
	            if (component != null) {
	            	s_dashboardGadgetFactoryCache.put(url, factory);
	            	return component;
	            }
        	}
        }
        
        return null;
	}
	
	/**
	 * Get chart renderer services
	 * @return list of {@link IChartRendererService}
	 */
	public static final List<IChartRendererService> getChartRendererServices() {
		return Service.locator().list(IChartRendererService.class).getServices();
	}

	private static IServiceReferenceHolder<IMappedFormFactory> s_mappedFormFactoryReference = null;

	/**
	 * Get mapped form factory service
	 * @return {@link IMappedFormFactory} instance
	 */
	public static IMappedFormFactory getMappedFormFactory(){
		IMappedFormFactory formFactoryService = null;
		if (s_mappedFormFactoryReference != null) {
			formFactoryService = s_mappedFormFactoryReference.getService();
			if (formFactoryService != null)
				return formFactoryService;
		}
		IServiceReferenceHolder<IMappedFormFactory> serviceReference = Service.locator().locate(IMappedFormFactory.class).getServiceReference();
		if (serviceReference != null) {
			formFactoryService = serviceReference.getService();
			s_mappedFormFactoryReference = serviceReference;
		}
		return formFactoryService;
	}

	/** AD_Window_ID:IQuickEntryFactory Reference */
	private final static CCache<Integer, IServiceReferenceHolder<IQuickEntryFactory>> s_quickEntryFactoryCache = new CCache<>(null, "IQuickEntryFactory", 100, false);
	
	/**
	 * Get quick entry instance
	 * @param AdWindowID AD_Window_ID
	 * @return AbstractWQuickEntry instance or null if AdWindowID not found
	 */
	public static AbstractWQuickEntry getQuickEntry(Integer AdWindowID) {
		IServiceReferenceHolder<IQuickEntryFactory> cache = s_quickEntryFactoryCache.get(AdWindowID);
		if (cache != null) {
			IQuickEntryFactory service = cache.getService();
			if (service != null) {
				AbstractWQuickEntry quickEntry = service.newQuickEntryInstance(AdWindowID);
				if (quickEntry != null)
					return quickEntry;
			}
			s_quickEntryFactoryCache.remove(AdWindowID);
		}
		List<IServiceReferenceHolder<IQuickEntryFactory>> factories = Service.locator().list(IQuickEntryFactory.class).getServiceReferences();
		if (factories != null) {
			for(IServiceReferenceHolder<IQuickEntryFactory> factory : factories) {
				IQuickEntryFactory service = factory.getService();
				if (service != null) {
					AbstractWQuickEntry quickEntry = service.newQuickEntryInstance(AdWindowID);
					if (quickEntry != null) {
						s_quickEntryFactoryCache.put(AdWindowID, factory);
						return quickEntry;
					}
				}
			}
		}
		return null;
	}	
	
	/**
	 * Get quick entry instance
	 * @param WindowNo 
	 * @param TabNo 
	 * @param AdWindowID AD_Window_ID
	 * @return AbstractWQuickEntry instance or null if AdWindowID not found
	 */
	public static AbstractWQuickEntry getQuickEntry(int WindowNo, int TabNo, int AdWindowID) {
		IServiceReferenceHolder<IQuickEntryFactory> cache = s_quickEntryFactoryCache.get(AdWindowID);
		if (cache != null) {
			IQuickEntryFactory service = cache.getService();
			if (service != null) {
				AbstractWQuickEntry quickEntry = service.newQuickEntryInstance(WindowNo, TabNo, AdWindowID);
				if (quickEntry != null)
					return quickEntry;
			}
			s_quickEntryFactoryCache.remove(AdWindowID);
		}
		List<IServiceReferenceHolder<IQuickEntryFactory>> factories = Service.locator().list(IQuickEntryFactory.class).getServiceReferences();
		if (factories != null) {
			for(IServiceReferenceHolder<IQuickEntryFactory> factory : factories) {
				IQuickEntryFactory service = factory.getService();
				if (service != null) {
					AbstractWQuickEntry quickEntry = service.newQuickEntryInstance(WindowNo, TabNo, AdWindowID);
					if (quickEntry != null) {
						s_quickEntryFactoryCache.put(AdWindowID, factory);
						return quickEntry;
					}
				}
			}
		}
		return null;
	}	
	
	/** CacheKey:IMediaViewProvider Reference. CacheKey is ContentType|File Extension */
	private static final CCache<String, IServiceReferenceHolder<IMediaViewProvider>> s_mediaViewProviderCache = new CCache<>("_IMediaViewProvider_Cache", "IMediaViewProvider", 100, false);
	
	/**
	 * Get media viewer service
	 * @param contentType
	 * @param extension
	 * @param mobile true for mobile, otherwise for desktop
	 * @return {@link IMediaView} instance
	 */
	public static IMediaView getMediaView(String contentType, String extension, boolean mobile) {
		String key = contentType + "|" + extension;
		
		IMediaView view = null;
		IServiceReferenceHolder<IMediaViewProvider> cache = s_mediaViewProviderCache.get(key);
		if (cache != null) {
			IMediaViewProvider service = cache.getService();
			if (service != null) {
				view = service.getMediaView(contentType, extension, mobile);
				if (view != null)
					return view;
			}
			s_mediaViewProviderCache.remove(key);
		}
		List<IServiceReferenceHolder<IMediaViewProvider>> serviceReferences = Service.locator().list(IMediaViewProvider.class).getServiceReferences();
		if (serviceReferences == null) 
			return null;
		for (IServiceReferenceHolder<IMediaViewProvider> serviceReference : serviceReferences)
		{
			IMediaViewProvider service = serviceReference.getService();
			if (service != null) {
				view = service.getMediaView(contentType, extension, mobile);
				if (view != null) {
					s_mediaViewProviderCache.put(key, serviceReference);
					return view;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Get IADTabpanel instance
	 * @param  tabType build in - FORM or SORT, custom - through IADTabPanelFactory extension
	 * @return {@link IADTabpanel} instance
	 */
	public static IADTabpanel getADTabPanel(String tabType)
	{
		IADTabpanel Object = null;
		List<IADTabPanelFactory> factoryList = Service.locator().list(IADTabPanelFactory.class).getServices();
		if (factoryList == null)
			return null;

		for (IADTabPanelFactory factory : factoryList)
		{
			Object = factory.getInstance(tabType);
			if (Object != null)
				return Object;
		}
		return null;
	} // getADTabPanel
	
	/**
	 * Get report viewer renderers
	 * @return list of {@link IReportViewerRenderer}
	 */
	public static List<IReportViewerRenderer> getReportViewerRenderers() {
		List<IServiceReferenceHolder<IReportViewerRenderer>> references = Service.locator().list(IReportViewerRenderer.class, null, null).getServiceReferences();
		return references.stream().filter(e -> e.getService() != null).map(e -> e.getService()).collect(Collectors.toCollection(ArrayList::new));
	}
	
	
	/**
	 * Get find window
	 * @param targetWindowNo
	 * @param targetTabNo
	 * @param title
	 * @param AD_Table_ID
	 * @param tableName
	 * @param whereExtended
	 * @param findFields
	 * @param minRecords
	 * @param adTabId
	 * @param windowPanel
	 * @return {@link FindWindow} instance
	 */
	public static FindWindow getFindWindow(int targetWindowNo, int targetTabNo, String title, int AD_Table_ID, String tableName, String whereExtended, GridField[] findFields, int minRecords, int adTabId, AbstractADWindowContent windowPanel) {
		
		IFindWindowFactory findWindowFactory = Service.locator().locate(IFindWindowFactory.class).getService();
	    return findWindowFactory.getInstance(targetWindowNo, targetTabNo, title, AD_Table_ID, tableName, whereExtended, findFields, minRecords, adTabId, windowPanel);
		
	}
	
	
}
