/*
 * Copyright (C) 2006-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */

package org.alfresco.jlan.smb.server.win32;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.jlan.debug.Debug;
import org.alfresco.jlan.netbios.NetBIOSName;
import org.alfresco.jlan.netbios.win32.NetBIOS;
import org.alfresco.jlan.netbios.win32.NetBIOSSocket;
import org.alfresco.jlan.netbios.win32.Win32NetBIOS;
import org.alfresco.jlan.netbios.win32.WinsockError;
import org.alfresco.jlan.netbios.win32.WinsockNetBIOSException;
import org.alfresco.jlan.server.NetworkServer;
import org.alfresco.jlan.server.SessionHandlerBase;
import org.alfresco.jlan.server.config.ServerConfiguration;
import org.alfresco.jlan.smb.mailslot.win32.Win32NetBIOSHostAnnouncer;
import org.alfresco.jlan.smb.server.CIFSConfigSection;
import org.alfresco.jlan.smb.server.PacketHandler;
import org.alfresco.jlan.smb.server.SMBServer;
import org.alfresco.jlan.smb.server.SMBSrvSession;

/**
 * Win32 NetBIOS Session Socket Handler Class
 * 
 * <p>
 * Uses the Win32 Netbios() call to provide the low level session layer for better integration with
 * Windows.
 * 
 * @author gkspencer
 */
public class Win32NetBIOSSessionSocketHandler extends SessionHandlerBase implements Runnable, LanaListener {

	// Constants
	//
	// Default LANA offline polling interval

	public static final long LANAPollingInterval = 5000; // 5 seconds

	// Thread group

	private static final ThreadGroup Win32NetBIOSGroup = new ThreadGroup("Win32NetBIOSSessions");

	// File server name

	private String m_srvName;

	// Accept connections from any clients or the named client only

	private byte[] m_acceptClient;
	private boolean m_acceptAny;
	private String m_acceptClientStr;

	// Local NetBIOS name to listen for sessions on and assigned name number

	private NetBIOSName m_nbName;

	private int m_nameNum;

	// Workstation NetBIOS name and assigned name number

	private NetBIOSName m_wksNbName;

	private int m_wksNameNum;

	// NetBIOS LAN adapter to use

	private int m_lana = -1;

	// Flag to indicate if the LANA is valid or the network adapter is currently
	// unplugged/offline/disabled

	private boolean m_lanaValid;

	// Polling interval in milliseconds to check if the configured LANA is back online

	private long m_lanaPoll;

	// Flag to indicate if we are using Win32 Netbios() or Winsock calls

	private boolean m_useWinsock;

	// Winsock Netbios socket to listen for incoming connections

	private NetBIOSSocket m_nbSocket;

	// Dummy socket used to register the workstation name that some clients search for, although
	// they connect to the file server service

	private NetBIOSSocket m_wksSocket;

	/**
	 * Class constructor
	 * 
	 * @param srv SMBServer
	 * @param debug boolean
	 */
	public Win32NetBIOSSessionSocketHandler(SMBServer srv, boolean debug) {

		super("Win32 NetBIOS", "SMB", srv, null, 0);

		// Enable/disable debug output

		setDebug(debug);

		// Get the Win32 NetBIOS file server name

		if ( srv.getCIFSConfiguration().getWin32ServerName() != null)
			m_srvName = srv.getCIFSConfiguration().getWin32ServerName();
		else
			m_srvName = srv.getCIFSConfiguration().getServerName();

		// Get the accepted client string, defaults to '*' to accept any client connection

		m_acceptClientStr = srv.getCIFSConfiguration().getWin32ClientAccept();
		NetBIOSName accName = new NetBIOSName(m_acceptClientStr, NetBIOSName.WorkStation, false);
		m_acceptClient = accName.getNetBIOSName();

		if ( srv.getCIFSConfiguration().getWin32ClientAccept().equals("*"))
			m_acceptAny = true;

		// Set the LANA to use, or -1 to use the first available

		m_lana = srv.getCIFSConfiguration().getWin32LANA();

		// Set the Win32 NetBIOS code to use either the Netbios() API call or Winsock NetBIOS calls

		m_useWinsock = srv.getCIFSConfiguration().useWinsockNetBIOS();

		// Debug

		if ( Debug.EnableInfo && hasDebug())
			Debug.println("[SMB] Win32 NetBIOS server " + m_srvName + " (using "
					+ (isUsingWinsock() ? "Winsock" : "Netbios() API") + ")");

		// Set the LANA offline polling interval

		m_lanaPoll = LANAPollingInterval;
	}

	/**
	 * Class constructor
	 * 
	 * @param srv SMBServer
	 * @param lana int
	 * @param debug boolean
	 */
	public Win32NetBIOSSessionSocketHandler(SMBServer srv, int lana, boolean debug) {

		super("Win32 NetBIOS", "SMB", srv, null, 0);

		// Enable/disable debug output

		setDebug(debug);

		// Get the Win32 NetBIOS file server name

		if ( srv.getCIFSConfiguration().getWin32ServerName() != null)
			m_srvName = srv.getCIFSConfiguration().getWin32ServerName();
		else
			m_srvName = srv.getCIFSConfiguration().getServerName();

		// Get the accepted client string, defaults to '*' to accept any client connection

		m_acceptClientStr = srv.getCIFSConfiguration().getWin32ClientAccept();
		NetBIOSName accName = new NetBIOSName(m_acceptClientStr, NetBIOSName.WorkStation, false);
		m_acceptClient = accName.getNetBIOSName();

		if ( srv.getCIFSConfiguration().getWin32ClientAccept().equals("*"))
			m_acceptAny = true;

		// Set the LANA to use, or -1 to use the first available

		m_lana = lana;

		// Set the Win32 NetBIOS code to use either the Netbios() API call or Winsock NetBIOS calls

		m_useWinsock = srv.getCIFSConfiguration().useWinsockNetBIOS();

		// Debug

		if ( Debug.EnableInfo && hasDebug())
			Debug.println("[SMB] Win32 NetBIOS server " + m_srvName + " (using "
					+ (isUsingWinsock() ? "Winsock" : "Netbios() API") + ")");

		// Set the LANA offline polling interval

		m_lanaPoll = LANAPollingInterval;
	}

	/**
	 * Return the LANA number that is being used
	 * 
	 * @return int
	 */
	public final int getLANANumber() {

		return m_lana;
	}

	/**
	 * Return the LANA offline polling interval to check for the LANA coming back online
	 * 
	 * @return long
	 */
	public final long getLANAOfflinePollingInterval() {

		return m_lanaPoll;
	}

	/**
	 * Return the assigned NetBIOS name number
	 * 
	 * @return int
	 */
	public final int getNameNumber() {

		return m_nameNum;
	}

	/**
	 * Return the local server name
	 * 
	 * @return String
	 */
	public final String getServerName() {

		return m_srvName;
	}

	/**
	 * Determine if Netbios() API calls or Winsock calls are being used
	 * 
	 * @return boolean
	 */
	public final boolean isUsingWinsock() {

		return m_useWinsock;
	}

	/**
	 * Initialize the session socket handler.
	 * 
	 * @param server NetworkServer
	 * @throws IOException
	 */
	public void initializeSessionHandler(NetworkServer server)
		throws IOException {

		// Enumerate the LAN adapters, use the first available if the LANA has not been specified in
		// the configuration

		int[] lanas = Win32NetBIOS.LanaEnumerate();
		if ( lanas.length > 0) {

			// Check if the LANA has been specified via the configuration, if not then use the first
			// available

			if ( m_lana == -1)
				m_lana = lanas[0];
			else {

				// Check if the required LANA is available

				boolean lanaOnline = false;
				int idx = 0;

				while (idx < lanas.length && lanaOnline == false) {

					// Check if the LANA is listed

					if ( lanas[idx++] == getLANANumber())
						lanaOnline = true;
				}

				// If the LANA is not available the main listener thread will poll the available
				// LANAs until the required LANA is available

				if ( lanaOnline == false) {

					// Indicate that the LANA is offline/unplugged/disabled

					m_lanaValid = false;
					return;
				}
			}
		}
		else {

			// If the LANA has not been set throw an exception as no LANAs are available

			if ( m_lana == -1)
				throw new IOException("No Win32 NetBIOS LANAs available");

			// The required LANA is offline/unplugged/disabled

			m_lanaValid = false;
			return;
		}

		// Create the local NetBIOS name to listen for incoming connections on

		m_nbName = new NetBIOSName(m_srvName, NetBIOSName.FileServer, false);
		m_wksNbName = new NetBIOSName(m_srvName, NetBIOSName.WorkStation, false);

		// Initialize the Win32 NetBIOS interface, either Winsock or Netbios() API

		if ( isUsingWinsock())
			initializeWinsockNetBIOS();
		else
			initializeNetbiosAPI();

		// Send a server event to indicate NetBIOS names have been added

		((SMBServer) getServer()).fireNetBIOSNamesAddedEvent(m_lana);

		// Indicate that the LANA is valid

		m_lanaValid = true;
	}

	/**
	 * Initialize the Win32 Netbios() API interface, add the server names
	 * 
	 * @exception IOException If the NetBIOS add name fails
	 */
	private final void initializeNetbiosAPI()
		throws IOException {

		// Reset the LANA

		Win32NetBIOS.Reset(m_lana);

		// Add the NetBIOS name to the local name table

		m_nameNum = Win32NetBIOS.AddName(m_lana, m_nbName.getNetBIOSName());
		if ( m_nameNum < 0)
			throw new IOException("Win32 NetBIOS AddName failed (file server), status = 0x" + Integer.toHexString(-m_nameNum)
					+ ", " + NetBIOS.getErrorString(-m_nameNum));

		// Register a NetBIOS name for the server name with the workstation name type, some clients
		// use this name to find the server

		m_wksNameNum = Win32NetBIOS.AddName(m_lana, m_wksNbName.getNetBIOSName());
		if ( m_wksNameNum < 0)
			throw new IOException("Win32 NetBIOS AddName failed (workstation), status = 0x" + Integer.toHexString(-m_wksNameNum)
					+ ", " + NetBIOS.getErrorString(-m_wksNameNum));
	}

	/**
	 * Initialize the Winsock NetBIOS interface
	 * 
	 * @exception IOException If a Winsock error occurs
	 */
	private final void initializeWinsockNetBIOS()
		throws IOException {

		// Create the NetBIOS listener socket, this will add the file server name

		m_nbSocket = NetBIOSSocket.createListenerSocket(getLANANumber(), m_nbName);

		// Create a NetBIOS socket using the workstation name, some clients search for this name

		m_wksSocket = NetBIOSSocket.createListenerSocket(getLANANumber(), m_wksNbName);
	}

	/**
	 * Check if the LANA is valid and accepting incoming sessions or the associated network adapter
	 * is unplugged/disabled/offline.
	 * 
	 * @return boolean
	 */
	public final boolean isLANAValid() {

		return m_lanaValid;
	}

	/**
	 * Close the session handler
	 * 
	 * @param server NetworkServer
	 */
	public void closeSessionHandler(NetworkServer server) {

		// Set the shutdown flag
		
		setShutdown( true);
		
		// Reset the LANA, if valid, to wake the main session listener thread

		if ( isLANAValid())
			Win32NetBIOS.Reset(m_lana);

		// If Winsock calls are being used close the sockets

		if ( isUsingWinsock()) {
			if ( m_nbSocket != null) {
				m_nbSocket.closeSocket();
				m_nbSocket = null;
			}

			if ( m_wksSocket != null) {
				m_wksSocket.closeSocket();
				m_wksSocket = null;
			}
		}
	}

	/**
	 * Run the NetBIOS session socket handler
	 */
	public void run() {

		try {

			// Clear the shutdown flag

			clearShutdown();

			// Wait for incoming connection requests

			while (hasShutdown() == false) {

				// Check if the LANA is valid and ready to accept incoming sessions

				if ( isLANAValid()) {

					// Wait for an incoming session request

					if ( isUsingWinsock()) {

						// Wait for an incoming session request using the Winsock NetBIOS interface

						runWinsock();
					}
					else {

						// Wait for an incoming session request using the Win32 Netbios() API
						// interface

						runNetBIOS();
					}
				}
				else {

					// Sleep for a short while ...

					try {
						Thread.sleep(getLANAOfflinePollingInterval());
					}
					catch (Exception ex) {
					}

					// Check if the network adapter/LANA is back online, if so then re-initialize
					// the LANA to start accepting sessions again

					try {
						initializeSessionHandler(getServer());
					}
					catch (Exception ex) {

						// DEBUG

						if ( Debug.EnableError && hasDebug()) {
							Debug.println("[SMB] Win32 NetBIOS Failed To ReInitialize LANA");
							Debug.println("  " + ex.getMessage());
						}
					}

					// DEBUG

					if ( Debug.EnableError && hasDebug() && isLANAValid())
						Debug.println("[SMB] Win32 NetBIOS LANA " + getLANANumber() + " Back Online");
				}
			}
		}
		catch (Exception ex) {

			// Do not report an error if the server has shutdown, closing the server socket
			// causes an exception to be thrown.

			if ( hasShutdown() == false) {
				Debug.println("[SMB] Win32 NetBIOS Server error : " + ex.toString());
				Debug.println(ex);
			}
		}

		// Debug

		if ( Debug.EnableInfo && hasDebug())
			Debug.println("[SMB] Win32 NetBIOS session handler closed");
	}

	/**
	 * Run the Win32 Netbios() API listen code
	 * 
	 * @exception Exception If an unhandled error occurs
	 */
	private final void runNetBIOS()
		throws Exception {

		// Debug

		if ( Debug.EnableInfo && hasDebug())
			Debug.println("[SMB] Waiting for Win32 NetBIOS session request (Netbios API) ...");

		// Clear the caller name

		byte[] callerNameBuf = new byte[NetBIOS.NCBNameSize];
		String callerName = null;

		callerNameBuf[0] = '\0';
		callerName = null;

		// Wait for a new NetBIOS session

		int lsn = Win32NetBIOS.Listen(m_lana, m_nbName.getNetBIOSName(), m_acceptClient, callerNameBuf);

		// Check if the session listener has been shutdown

		if ( hasShutdown())
			return;

		// Get the caller name, if available

		if ( callerNameBuf[0] != '\0')
			callerName = new String(callerNameBuf).trim();
		else
			callerName = "";

		// Create a packet handler and thread for the new session

		if ( lsn >= 0) {

			// Create a new session thread

			try {

				// Debug

				if ( Debug.EnableInfo && hasDebug())
					Debug.println("[SMB] Win32 NetBIOS session request received, lsn=" + lsn + ", caller=[" + callerName + "]");

				// Check if the connection should be accepted

				if ( acceptCaller(callerName)) {

					// Create a packet handler for the session

					SMBServer smbServer = (SMBServer) getServer();
					PacketHandler pktHandler = new Win32NetBIOSPacketHandler(m_lana, lsn, callerName, smbServer.getPacketPool());

					// Create a server session for the new request, and set the session id.

					SMBSrvSession srvSess = SMBSrvSession.createSession(pktHandler, smbServer, getNextSessionId());

					// Start the new session in a seperate thread

					Thread srvThread = new Thread(Win32NetBIOSGroup, srvSess);
					srvThread.setDaemon(true);
					srvThread.setName("Sess_W" + srvSess.getSessionId() + "_LSN" + lsn);
					srvThread.start();
				}
				else {

					// DEBUG

					if ( Debug.EnableDbg && hasDebug())
						Debug.println("[SMB] Win32 NetBIOS Reject client " + callerName);

					// Hangup the session

					Win32NetBIOS.Hangup(m_lana, lsn);
				}
			}
			catch (Exception ex) {

				// Debug

				if ( Debug.EnableError && hasDebug())
					Debug.println("[SMB] Win32 NetBIOS Failed to create session, " + ex.toString());
			}
		}
		else {

			// Check if the error indicates the network adapter is
			// unplugged/offline/disabled

			int sts = -lsn;

			if ( sts == NetBIOS.NRC_Bridge) {

				// Indicate that the LANA is no longer valid

				m_lanaValid = false;

				// DEBUG

				if ( Debug.EnableError && hasDebug())
					Debug.println("[SMB] Win32 NetBIOS LANA offline/disabled, LANA=" + getLANANumber());
			}
			else if ( Debug.EnableError && hasDebug())
				Debug.println("[SMB] Win32 NetBIOS Listen error, 0x" + Integer.toHexString(-lsn) + ", "
						+ NetBIOS.getErrorString(-lsn));
		}
	}

	/**
	 * Run the Winsock NetBIOS listen code
	 * 
	 * @exception Exception If an unhandled error occurs
	 */
	private final void runWinsock()
		throws Exception {

		// Debug

		if ( Debug.EnableInfo && hasDebug())
			Debug.println("[SMB] Waiting for Win32 NetBIOS session request (Winsock) ...");

		// Wait for a new NetBIOS session

		NetBIOSSocket sessSock = null;

		try {

			// Wait for an incoming session connection

			sessSock = m_nbSocket.accept();
		}
		catch (WinsockNetBIOSException ex) {

			// Check if the network is down

			if ( ex.getErrorCode() == WinsockError.WsaENetDown) {

				// Check if the LANA we are listening on is no longer valid

				if ( isLANAOnline(m_lana) == false) {

					// Network/LANA is offline, cleanup the current listening sockets and wait for
					// the LANA to come back online

					if ( m_nbSocket != null) {
						m_nbSocket.closeSocket();
						m_nbSocket = null;
					}

					if ( m_wksSocket != null) {
						m_wksSocket.closeSocket();
						m_wksSocket = null;
					}

					// Indciate that the LANA is no longer valid

					m_lanaValid = false;

					// Debug

					if ( Debug.EnableError && hasDebug())
						Debug.println("[SMB] Winsock NetBIOS network down, LANA=" + m_lana);
				}
			}
			else {

				// Debug

				if ( hasShutdown() == false && Debug.EnableError && hasDebug())
					Debug.println("[SMB] Winsock NetBIOS listen error, " + ex.getMessage());
			}
		}

		// Check if the session listener has been shutdown

		if ( hasShutdown())
			return;

		// Create a packet handler and thread for the new session

		if ( sessSock != null) {

			// Create a new session thread

			try {

				// Debug

				if ( Debug.EnableInfo && hasDebug())
					Debug.println("[SMB] Winsock NetBIOS session request received, caller=" + sessSock.getName());

				// Check if the connection should be accepted

				if ( acceptCaller(sessSock.getName())) {

					// Create a packet handler for the session

					SMBServer smbServer = (SMBServer) getServer();
					PacketHandler pktHandler = new WinsockNetBIOSPacketHandler(m_lana, sessSock, smbServer.getPacketPool(), false);

					// Create a server session for the new request, and set the session id.

					SMBSrvSession srvSess = SMBSrvSession.createSession(pktHandler, smbServer, getNextSessionId());

					// Start the new session in a seperate thread

					Thread srvThread = new Thread(Win32NetBIOSGroup, srvSess);
					srvThread.setDaemon(true);
					srvThread.setName("Sess_WS" + srvSess.getSessionId());
					srvThread.start();
				}
				else {

					// DEBUG

					if ( Debug.EnableDbg && hasDebug())
						Debug.println("[SMB] Winsock NetBIOS Reject client " + sessSock.getName());

					// Close the session

					sessSock.closeSocket();
				}
			}
			catch (Exception ex) {

				// Debug

				if ( Debug.EnableError && hasDebug())
					Debug.println("[SMB] Winsock NetBIOS Failed to create session, " + ex.toString());
			}
		}
	}

	/**
	 * Create the Win32 NetBIOS session socket handlers for the main SMB/CIFS server
	 * 
	 * @param server SMBServer
	 * @param sockDbg boolean
	 */
	public final static void createSessionHandlers(SMBServer server, boolean sockDbg) {

		// Access the server configuration

		ServerConfiguration config = server.getConfiguration();
		CIFSConfigSection cifsConfig = (CIFSConfigSection) config.getConfigSection(CIFSConfigSection.SectionName);

		// DEBUG

		if ( Debug.EnableInfo && sockDbg) {
			int[] lanas = Win32NetBIOS.LanaEnumerate();

			StringBuffer lanaStr = new StringBuffer();
			if ( lanas != null && lanas.length > 0) {
				for (int i = 0; i < lanas.length; i++) {
					lanaStr.append(Integer.toString(lanas[i]));
					lanaStr.append(" ");
				}
			}
			Debug.println("[SMB] Win32 NetBIOS Available LANAs: " + lanaStr.toString());
		}

		// Check if the Win32 NetBIOS session handler should use a particular LANA/network adapter
		// or should use all available LANAs/network adapters (that have NetBIOS enabled).

		Win32NetBIOSSessionSocketHandler sessHandler = null;
		List<Win32NetBIOSSessionSocketHandler> lanaListeners = new ArrayList<Win32NetBIOSSessionSocketHandler>();

		if ( cifsConfig.getWin32LANA() != -1) {

			// Create a single Win32 NetBIOS session handler using the specified LANA

			sessHandler = new Win32NetBIOSSessionSocketHandler(server, cifsConfig.getWin32LANA(), sockDbg);

			try {
				sessHandler.initializeSessionHandler(server);
			}
			catch (Exception ex) {

				// DEBUG

				if ( Debug.EnableError && sockDbg) {
					Debug.println("[SMB] Win32 NetBIOS failed to create session handler for LANA " + cifsConfig.getWin32LANA());
					Debug.println("      " + ex.getMessage());
				}
			}

			// Add the session handler to the SMB/CIFS server

			// server.addSessionHandler(sessHandler);

			// Run the NetBIOS session handler in a seperate thread

			Thread nbThread = new Thread(sessHandler);
			nbThread.setName("Win32NB_Handler_" + cifsConfig.getWin32LANA());
			nbThread.start();

			// DEBUG

			if ( Debug.EnableInfo && sockDbg)
				Debug.println("[SMB] Win32 NetBIOS created session handler on LANA " + cifsConfig.getWin32LANA());

			// Check if a host announcer should be enabled

			if ( cifsConfig.hasWin32EnableAnnouncer()) {

				// Create a host announcer

				Win32NetBIOSHostAnnouncer hostAnnouncer = new Win32NetBIOSHostAnnouncer(sessHandler, cifsConfig.getDomainName(),
						cifsConfig.getWin32HostAnnounceInterval());
				hostAnnouncer.setDebug(sockDbg);

				// Add the host announcer to the SMB/CIFS server list

				// server.addHostAnnouncer(hostAnnouncer);
				hostAnnouncer.start();

				// DEBUG

				if ( Debug.EnableInfo && sockDbg)
					Debug.println("[SMB] Win32 NetBIOS host announcer enabled on LANA " + cifsConfig.getWin32LANA());
			}

			// Check if the session handler implements the LANA listener interface

			if ( sessHandler instanceof LanaListener)
				lanaListeners.add(sessHandler);
		}
		else {

			// Get a list of the available LANAs

			int[] lanas = Win32NetBIOS.LanaEnumerate();

			if ( lanas != null && lanas.length > 0) {

				// Create a session handler for each available LANA

				for (int i = 0; i < lanas.length; i++) {

					// Get the current LANA

					int lana = lanas[i];

					// Create a session handler

					sessHandler = new Win32NetBIOSSessionSocketHandler(server, lana, sockDbg);

					try {
						sessHandler.initializeSessionHandler(server);
					}
					catch (Exception ex) {

						// DEBUG

						if ( Debug.EnableError && sockDbg) {
							Debug.println("[SMB] Win32 NetBIOS failed to create session handler for LANA " + lana);
							Debug.println("      " + ex.getMessage());
						}
					}

					// Add the session handler to the SMB/CIFS server

					// server.addSessionHandler(sessHandler);

					// Run the NetBIOS session handler in a seperate thread

					Thread nbThread = new Thread(sessHandler);
					nbThread.setName("Win32NB_Handler_" + lana);
					nbThread.start();

					// DEBUG

					if ( Debug.EnableError && sockDbg)
						Debug.println("[SMB] Win32 NetBIOS created session handler on LANA " + lana);

					// Check if a host announcer should be enabled

					if ( cifsConfig.hasWin32EnableAnnouncer()) {

						// Create a host announcer

						Win32NetBIOSHostAnnouncer hostAnnouncer = new Win32NetBIOSHostAnnouncer(sessHandler, cifsConfig
								.getDomainName(), cifsConfig.getWin32HostAnnounceInterval());
						hostAnnouncer.setDebug(sockDbg);

						// Add the host announcer to the SMB/CIFS server list

						// server.addHostAnnouncer(hostAnnouncer);
						hostAnnouncer.start();

						// DEBUG

						if ( Debug.EnableInfo && sockDbg)
							Debug.println("[SMB] Win32 NetBIOS host announcer enabled on LANA " + lana);
					}

					// Check if the session handler implements the LANA listener interface

					if ( sessHandler instanceof LanaListener)
						lanaListeners.add(sessHandler);
				}
			}

			// Create a LANA monitor to check for new LANAs becoming available

			Win32NetBIOSLanaMonitor lanaMonitor = new Win32NetBIOSLanaMonitor(server, lanas, LANAPollingInterval, sockDbg);

			// Register any session handlers that are LANA listeners

			if ( lanaListeners.size() > 0) {

				for (int i = 0; i < lanaListeners.size(); i++) {

					// Get the current LANA listener

					Win32NetBIOSSessionSocketHandler handler = lanaListeners.get(i);

					// Register the LANA listener

					lanaMonitor.addLanaListener(handler.getLANANumber(), handler);
				}
			}
		}
	}

	/**
	 * Check if the specified LANA is online
	 * 
	 * @param lana int
	 * @return boolean
	 */
	private final boolean isLANAOnline(int lana) {

		// Get a list of the available LANAs

		int[] lanas = Win32NetBIOS.LanaEnumerate();

		if ( lanas != null && lanas.length > 0) {

			// Check if the specified LANA is available

			for (int i = 0; i < lanas.length; i++) {
				if ( lanas[i] == lana)
					return true;
			}
		}

		// LANA not online

		return false;
	}

	/**
	 * LANA listener status change callback
	 * 
	 * @param lana int
	 * @param online boolean
	 */
	public void lanaStatusChange(int lana, boolean online) {

		// If the LANA has gone offline, close the listening socket and wait for the LANA to
		// come back online

		if ( online == false) {

			// Indicate that the LANA is offline

			m_lanaValid = false;

			// Close the listening sockets

			if ( m_nbSocket != null) {
				m_nbSocket.closeSocket();
				m_nbSocket = null;
			}

			if ( m_wksSocket != null) {
				m_wksSocket.closeSocket();
				m_wksSocket = null;
			}
		}
	}

	/**
	 * Check if the new session should be accepted, if we are only allowing connections from a
	 * specific name the session may be rejected.
	 * 
	 * @param callerName String
	 * @return boolean
	 */
	private final boolean acceptCaller(String callerName) {

		// Check if we are accepting connections from any client

		if ( m_acceptAny)
			return true;

		// Check if the caller name matches the accept name

		if ( callerName.equalsIgnoreCase(m_acceptClientStr))
			return true;

		// Reject the client

		return false;
	}

	/**
	 * Check if the new session should be accepted, if we are only allowing connections from a
	 * specific name the session may be rejected.
	 * 
	 * @param callerNBName NetBIOSName
	 * @return boolean
	 */
	private final boolean acceptCaller(NetBIOSName callerNBName) {

		// Check if we are accepting connections from any client

		if ( m_acceptAny)
			return true;

		// Check if the caller name matches the accept name

		byte[] callerName = callerNBName.getNetBIOSName();

		for (int i = 0; i < NetBIOS.NCBNameSize - 1; i++) {
			if ( callerName[i] != m_acceptClient[i])
				return false;
		}

		// Accept the client

		return true;
	}
}
