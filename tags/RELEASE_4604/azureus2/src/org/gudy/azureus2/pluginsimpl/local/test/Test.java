/*
 * Created on 02-Jan-2005
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.local.test;


import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.ddb.*;
import org.gudy.azureus2.plugins.disk.DiskManager;
import org.gudy.azureus2.plugins.disk.DiskManagerException;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.disk.DiskManagerReadRequest;
import org.gudy.azureus2.plugins.disk.DiskManagerReadRequestListener;
import org.gudy.azureus2.plugins.disk.DiskManagerWriteRequest;
import org.gudy.azureus2.plugins.disk.DiskManagerWriteRequestListener;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadPeerListener;
import org.gudy.azureus2.plugins.messaging.Message;
import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.messaging.MessageManager;
import org.gudy.azureus2.plugins.messaging.MessageManagerListener;
import org.gudy.azureus2.plugins.messaging.bittorrent.BTMessageCancel;
import org.gudy.azureus2.plugins.messaging.bittorrent.BTMessagePiece;
import org.gudy.azureus2.plugins.messaging.bittorrent.BTMessageRequest;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnection;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnectionListener;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageEndpoint;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageHandler;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageRegistration;
import org.gudy.azureus2.plugins.network.IncomingMessageQueueListener;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.peers.PeerManagerEvent;
import org.gudy.azureus2.plugins.peers.PeerManagerListener2;
import org.gudy.azureus2.plugins.peers.Piece;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.torrent.TorrentAttributeEvent;
import org.gudy.azureus2.plugins.torrent.TorrentAttributeListener;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.plugins.utils.Utilities;
import org.gudy.azureus2.plugins.utils.search.Search;
import org.gudy.azureus2.plugins.utils.search.SearchInitiator;
import org.gudy.azureus2.plugins.utils.search.SearchListener;
import org.gudy.azureus2.plugins.utils.search.SearchProvider;
import org.gudy.azureus2.plugins.utils.search.SearchProviderResults;
import org.gudy.azureus2.plugins.utils.search.SearchResult;
import org.gudy.azureus2.plugins.utils.security.SEPublicKey;
import org.gudy.azureus2.plugins.utils.security.SEPublicKeyLocator;
import org.gudy.azureus2.plugins.utils.security.SESecurityManager;
import org.gudy.azureus2.plugins.utils.subscriptions.Subscription;
import org.gudy.azureus2.plugins.utils.subscriptions.SubscriptionManager;
import org.gudy.azureus2.plugins.utils.subscriptions.SubscriptionResult;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.security.CryptoManagerPasswordHandler;
import com.aelitis.azureus.core.security.CryptoManagerPasswordHandler.passwordDetails;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;


/**
 * @author parg
 *
 */


public class 
Test 
	implements Plugin
{

	protected PluginInterface		plugin_interface;
	
	public void 
	initialize(
		PluginInterface _pi )
	{	
		plugin_interface	= _pi;
		
		plugin_interface.addListener(
				new PluginListener()
				{
					public void
					initializationComplete()
					{
						Thread	t  = 
							new AEThread("test")
							{
								public void
								runSupport()
								{
									
									testBTMessageHandler();
	
								}
							};
							
						t.setDaemon(true);
						
						t.start();
					}
					
					public void
					closedownInitiated()
					{	
					}
					
					public void
					closedownComplete()
					{
					}
				});
	}
	
	private void
	testBTMessageHandler()
	{
		try{
			MessageManager man = plugin_interface.getMessageManager();
			
			man.registerMessageType( new BorkMessage( "" ));

			man.locateCompatiblePeers(
				plugin_interface, 
				new TestMessage( "" ), 
				new MessageManagerListener()
				{
					public void 
					compatiblePeerFound( 
						Download 	download, 
						final Peer 		peer, 
						Message 	message )
					{
						System.out.println( "Compatible peer found: " + peer.getIp());
						
						peer.getConnection().getIncomingMessageQueue().registerPriorityListener(
							new IncomingMessageQueueListener()
							{
								 public boolean 
								 messageReceived( 
									Message message )
								 {
									 System.out.println( peer.getIp() + ": " + message.getDescription() + " (" + message + ")");
									 
									 if ( message instanceof BorkMessage ){
										
										 System.out.println( "Got a borker: " + ((BorkMessage)message).getArg());
										 
										 return( true );
									 }
									 
									 return( false );
								 }
								  
								 public void 
								 bytesReceived( 
										int byte_count )
								 {							 
								 }
							});
						
						//peer.getConnection().getOutgoingMessageQueue().sendMessage( new BorkMessage( "Hello Mr Borker" ));
					}

					public void 
					peerRemoved( 
						Download 	download, 
						Peer 		peer )
					{					
					}
				});
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
		
		/*
		plugin_interface.getDownloadManager().addListener(
			new DownloadManagerListener()
			{
				public void
				downloadAdded(
					Download	download )
				{
					download.addPeerListener(
						new DownloadPeerListener()
						{
							public void
							peerManagerAdded(
								final Download			download,
								final PeerManager		peer_manager )
							{
								peer_manager.addListener(
									new PeerManagerListener2()
									{
										public void
										eventOccurred(
											PeerManagerEvent	event )
										{
											if ( event.getType() == PeerManagerEvent.ET_PEER_ADDED ){
												
												event.getPeer().getConnection().getIncomingMessageQueue().registerPriorityListener(
													new IncomingMessageQueueListener()
													{
														 public boolean 
														 messageReceived( 
															Message message )
														 {
															 if ( message instanceof BTMessageRequest ){
																 
																 BTMessageRequest request = (BTMessageRequest)message;
																 
																 System.out.println( "BT_REQUEST: " + request.getPieceNumber() + "/" + request.getPieceOffset() + "/" + request.getLength());
																 
															 }else if ( message instanceof BTMessageCancel ){
																 
																 BTMessageCancel cancel = (BTMessageCancel)message;
																 
																 System.out.println( "BT_CANCEL: " + cancel.getPieceNumber() + "/" + cancel.getPieceOffset() + "/" + cancel.getLength());

															 }else if ( message instanceof BTMessagePiece ){
																
																 BTMessagePiece piece = (BTMessagePiece)message;
																 
																 System.out.println( "BT_PIECE: " + piece.getPieceNumber() + "/" + piece.getPieceOffset() + "/" + piece.getPieceData().remaining());
															 }
															 
															 return( false );
														 }
														  
														 public void 
														 bytesReceived( 
																int byte_count )
														 {
															 
														 }
													});
											}
										}
									});
							}
							
							public void
							peerManagerRemoved(
								Download		download,
								PeerManager		peer_manager )
							{
								
							}
						});
				}
				
				public void
				downloadRemoved(
					Download	download )
				{
					
				}
			});
			*/
	}
	
	private class
	BorkMessage
		implements Message
	{
		private ByteBuffer 	buffer;
		private String		arg;
		
		private
		BorkMessage(
			String		_arg )
		{
			arg		= _arg;
			
			try{
			    byte[] arg_bytes = arg.getBytes( "UTF-8" );
			    
			    buffer = ByteBuffer.allocate( 4 + arg_bytes.length );
			    
			    buffer.putInt( arg_bytes.length );
			    
			    buffer.put( arg_bytes );
			    
			    buffer.flip();
			    
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
		
		public String 
		getID()
		{
			return( "BORK_MESSAGE" );
		}

		public int 
		getType()
		{
			return( TYPE_PROTOCOL_PAYLOAD );
		}

		public String 
		getDescription()
		{
			return( "borker message" );
		}

		public ByteBuffer[] 
		getPayload()
		{
			return new ByteBuffer[] { buffer };  
		}

		public String
		getArg()
		{
			return( arg );
		}

		public Message 
		create( 
			ByteBuffer data ) 

			throws MessageException
		{
			try{
				int size = data.getInt();
	
				byte[] bytes = new byte[ size ];
				
				data.get( bytes );
				
				return( new BorkMessage(new String(bytes, "UTF-8" )));
				
			}catch( Throwable e ){
				
				throw( new MessageException( "create failed", e ));
			}
		}
		
		public void 
		destroy()
		{
		}
	}
	
	private class
	TestMessage
		implements Message
	{
		private ByteBuffer 	buffer;
		private String		arg;
		
		private
		TestMessage(
			String		_arg )
		{
			arg		= _arg;
			
			try{
			    byte[] arg_bytes = arg.getBytes( "UTF-8" );
			    
			    buffer = ByteBuffer.allocate( 4 + arg_bytes.length );
			    
			    buffer.putInt( arg_bytes.length );
			    
			    buffer.put( arg_bytes );
			    
			    buffer.flip();
			    
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
		
		public String 
		getID()
		{
			return( "BT_REQUEST" );
		}

		public int 
		getType()
		{
			return( TYPE_PROTOCOL_PAYLOAD );
		}

		public String 
		getDescription()
		{
			return( "borker message" );
		}

		public ByteBuffer[] 
		getPayload()
		{
			return new ByteBuffer[] { buffer };  
		}

		public String
		getArg()
		{
			return( arg );
		}

		public Message 
		create( 
			ByteBuffer data ) 

			throws MessageException
		{
			try{
				int size = data.getInt();
	
				byte[] bytes = new byte[ size ];
				
				data.get( bytes );
				
				return( new BorkMessage(new String(bytes, "UTF-8" )));
				
			}catch( Throwable e ){
				
				throw( new MessageException( "create failed", e ));
			}
		}
		
		public void 
		destroy()
		{
		}
	}
	
	private void
	testPluginWrite()
	{
		plugin_interface.getDownloadManager().addListener(
				new DownloadManagerListener()
				{
					public void
					downloadAdded(
						Download	download )
					{
						download.addPeerListener(
							new DownloadPeerListener()
							{
								public void
								peerManagerAdded(
									final Download			download,
									final PeerManager		peer_manager )
								{
									new AEThread2( "" )
									{
										public void
										run()
										{
											DiskManager dm  = peer_manager.getDiskManager();
																						
											File f = new File( "C:\\temp\\capture2.pcap" );
											
											long	len = f.length();
											
											int	piece_size 	= (int)download.getTorrent().getPieceSize();

											int	max_blocks = piece_size / DiskManager.BLOCK_SIZE;
											
											List<long[]> chunks = new ArrayList<long[]>();
											
											long i = 0;
											
											while( i < len ){
												
												int		blocks = ((int)(Math.random()*max_blocks))+1;
												
												long	rem_in_piece = i - ((i/piece_size)*piece_size);
												
												if ( rem_in_piece == 0 ){
													
													rem_in_piece = piece_size;
												}
												
												rem_in_piece = Math.min( len-i, rem_in_piece);
												
												long	chunk_size = Math.min(rem_in_piece,blocks*DiskManager.BLOCK_SIZE);
												
												chunks.add( new long[]{ i, chunk_size });
												
												i += chunk_size;
											}
																				
											Utilities utils = plugin_interface.getUtilities();
											
											try{
												RandomAccessFile raf = new RandomAccessFile(f, "r" );
												
												while( !chunks.isEmpty()){
													
													long[] chunk = chunks.remove((int)(Math.random()*chunks.size()));
													
													final long	position 	= chunk[0];
													final int	size		= (int)chunk[1];
													
													raf.seek( position );
													
													byte[] buffer = new byte[ size ];
													
													raf.read( buffer );
													
													int	piece_number 	= (int)(position/piece_size);
													int	piece_offset	= (int)(position % piece_size);
																							
													dm.write(
														piece_number,
														piece_offset,
														utils.allocatePooledByteBuffer( buffer ),
														new DiskManagerWriteRequestListener()
														{
															public void
															complete(
																DiskManagerWriteRequest		request )
															{
																System.out.println( "write ok: offset=" + position + ",size=" + size );
															}
															
															public void
															failed(
																DiskManagerWriteRequest		request,
																DiskManagerException		error )
															{
																System.out.println( "write failed" );
																
																error.printStackTrace();
															}
														});
													
													Thread.sleep(10);
												}
											}catch( Throwable e ){
												
												e.printStackTrace();
											}
										}
									}.start();
								}
								
								public void
								peerManagerRemoved(
									Download		download,
									PeerManager		peer_manager )
								{
									
								}
							});
					}
					
					public void
					downloadRemoved(
						Download	download )
					{
						
					}
				});
	}
	
	private void
	testPieceListener()
	{
		plugin_interface.getDownloadManager().addListener(
			new DownloadManagerListener()
			{
				public void
				downloadAdded(
					Download	download )
				{
					download.addPeerListener(
						new DownloadPeerListener()
						{
							public void
							peerManagerAdded(
								Download				download,
								final PeerManager		peer_manager )
							{
								peer_manager.addListener(
									new PeerManagerListener2()
									{
										public void
										eventOccurred(
											PeerManagerEvent	event )
										{
											if ( event.getType() == PeerManagerEvent.ET_PIECE_COMPLETION_CHANGED ){
												
												Piece piece = (Piece)event.getData();
												
												System.out.println( "piece: " + piece.getIndex() + ", done=" + piece.isDone());
												
												try{
													peer_manager.getDiskManager().read(
														piece.getIndex(),
														0,
														piece.getLength(),
														new DiskManagerReadRequestListener()
														{
															public void
															complete(
																DiskManagerReadRequest		request,
																PooledByteBuffer			buffer )
															{
																System.out.println( "    read ok" );
																
																buffer.returnToPool();
															}
															
															public void
															failed(
																DiskManagerReadRequest		request,
																DiskManagerException		error )
															{
																System.out.println( "    read failed: " + Debug.getNestedExceptionMessage( error ));
															}
														});
												}catch( Throwable e ){
												
													e.printStackTrace();
												}
											}
										}
									});
							}
							
							public void
							peerManagerRemoved(
								Download		download,
								PeerManager		peer_manager )
							{
								
							}
						});
				}
				
				public void
				downloadRemoved(
					Download	download )
				{
					
				}
			});
	}
	
	private void
	testSubs()
	{
		try{
			SubscriptionManager sm = plugin_interface.getUtilities().getSubscriptionManager();
		
			Subscription[] subs = sm.getSubscriptions();
			
			for ( Subscription s: subs ){
				
				System.out.println( "subs: " + s.getName());
				
				SubscriptionResult[] results = s.getResults();
				
				for ( SubscriptionResult result: results ){
					
					System.out.println( "    " + result.getProperty( SearchResult.PR_NAME ) + ", read=" + result.isRead());
				}
				
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	private void
	testMessaging()
	{
		try{
			AzureusCoreFactory.getSingleton().getCryptoManager().addPasswordHandler(
				new CryptoManagerPasswordHandler()
				{
					public int
					getHandlerType()
					{
						return( HANDLER_TYPE_USER );
					}
					
					public passwordDetails
		        	getPassword(
		        		int			handler_type,
		        		int			action_type,
		        		boolean		last_pw_incorrect,
		        		String		reason )
					{
						System.out.println( "CryptoPassword (" + reason + ")");
						
						return(
							new passwordDetails()
							{
								public char[]
								getPassword()
								{
									return( "changeit".toCharArray());
								}
								
								public int 
								getPersistForSeconds() 
								{
									return( 0 );
								}
							});
					}
					
					public void 
					passwordOK(
						int 				handler_type,
						passwordDetails 	details) 
					{
					}
				});
			
			final SESecurityManager	sec_man = plugin_interface.getUtilities().getSecurityManager();
			
			final SEPublicKey	my_key = sec_man.getPublicKey( SEPublicKey.KEY_TYPE_ECC_192, "test" );

			final int	stream_crypto 	= MessageManager.STREAM_ENCRYPTION_RC4_REQUIRED;
			final boolean	use_sts		= true;
			final int	block_crypto 	= SESecurityManager.BLOCK_ENCRYPTION_AES;
			
			GenericMessageRegistration	reg = 
				plugin_interface.getMessageManager().registerGenericMessageType(
					"GENTEST", "Gen test desc", 
					stream_crypto,
					new GenericMessageHandler()
					{
						public boolean
						accept(
							GenericMessageConnection	connection )
						
							throws MessageException
						{
							System.out.println( "accept" );
							
							try{
								if ( use_sts ){
									
									connection = sec_man.getSTSConnection(
											connection, 
											my_key,
											new SEPublicKeyLocator()
											{
												public boolean
												accept(
													Object		context,
													SEPublicKey	other_key )
												{
													System.out.println( "acceptKey" );
													
													return( true );
												}
											},
											"test",
											block_crypto );
								}
										
								connection.addListener(
									new GenericMessageConnectionListener()
									{
										public void
										connected(
											GenericMessageConnection	connection )
										{
										}
										
										public void
										receive(
											GenericMessageConnection	connection,
											PooledByteBuffer			message )
										
											throws MessageException
										{
											System.out.println( "receive: " + message.toByteArray().length );
											
											PooledByteBuffer	reply = 
												plugin_interface.getUtilities().allocatePooledByteBuffer( 
														new byte[connection.getMaximumMessageSize()]);
											
											connection.send( reply );
										}
										
										public void
										failed(
											GenericMessageConnection	connection,
											Throwable 					error )
										
											throws MessageException
										{
											System.out.println( "Responder connection error:" );

											error.printStackTrace();
										}	
									});
								
							}catch( Throwable e ){
								
								connection.close();
								
								e.printStackTrace();
							}
							
							return( true );
						}
					});
			
			InetSocketAddress	tcp_target = new InetSocketAddress( "127.0.0.1", 		6889 );
			InetSocketAddress	udp_target = new InetSocketAddress( "212.159.18.92", 	6881 );
			
			GenericMessageEndpoint	endpoint = reg.createEndpoint( tcp_target );
			
			endpoint.addTCP( tcp_target );
			endpoint.addUDP( udp_target );
			
			while( true ){
				
				try{
					for (int i=0;i<1000;i++){
						
						System.out.println( "Test: initiating connection" );
						
						final AESemaphore	sem = new AESemaphore( "wait!" );
						
						GenericMessageConnection	con = reg.createConnection( endpoint );
						
						if ( use_sts ){
							
							con = sec_man.getSTSConnection( 
								con, my_key,
								new SEPublicKeyLocator()
								{
									public boolean
									accept(
										Object			context,
										SEPublicKey		other_key )
									{
										System.out.println( "acceptKey" );
										
										return( true );
									}
								},
								"test", block_crypto );
						}
						
						con.addListener(
							new GenericMessageConnectionListener()
							{
								public void
								connected(
									GenericMessageConnection	connection )
								{
									System.out.println( "connected" );
									
									PooledByteBuffer	data = plugin_interface.getUtilities().allocatePooledByteBuffer( "1234".getBytes());
									
									try{
										connection.send( data );
										
									}catch( Throwable e ){
										
										e.printStackTrace();
									}
								}
								
								public void
								receive(
									GenericMessageConnection	connection,
									PooledByteBuffer			message )
								
									throws MessageException
								{
									System.out.println( "receive: " + message.toByteArray().length );
									
									
									try{
										Thread.sleep(30000);
									}catch( Throwable e ){
										
									}
								
									/*
									PooledByteBuffer	reply = 
										plugin_interface.getUtilities().allocatePooledByteBuffer( new byte[16*1024]);
									
									
									connection.send( reply );
									*/
									
									System.out.println( "closing connection" );
									
									connection.close();
									
									sem.release();
								}
								
								public void
								failed(
									GenericMessageConnection	connection,
									Throwable 					error )
								
									throws MessageException
								{
									System.out.println( "Initiator connection error:" );
									
									error.printStackTrace();
									
									sem.release();
								}
							});
						
			
						con.connect();
						
						sem.reserve();
						
						Thread.sleep( 1000 );
					}
				
				}catch( Throwable e ){
					
					e.printStackTrace();
					
					try{
						System.out.println( "Sleeping before retrying" );
						
						Thread.sleep( 30000 );
						
					}catch( Throwable f ){
					}
				}
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	private void
	testSearch()
	{
		while( true ){
			
			try{
				Thread.sleep(1000);
				
				SearchInitiator si = plugin_interface.getUtilities().getSearchInitiator();

				SearchProvider[] providers = si.getProviders();
				
				System.out.println( "search providers=" + providers.length );
				
				if ( providers.length > 0 ){
					
					Map<String,String> properties = new HashMap<String,String>();
				
					properties.put( SearchInitiator.PR_SEARCH_TERM, "monkey" );
					properties.put( SearchInitiator.PR_MATURE, "true" );
					
					/*
					 * 
					final boolean[] complete = {false};

					Search s = 
						si.createSearch(
							providers,
							properties,
							new SearchListener()
							{
								public void
								receivedResults(
									SearchProviderResults[]		results )
								{
									System.out.println( "received results" );
									
									for ( SearchProviderResults result: results ){
										
										System.out.println( "    " + result.getProvider().getProperty( SearchProvider.PR_NAME ) + ": comp=" + result.isComplete() + ", error=" + result.getError());
										
										SearchResult[] srs = result.getResults();
										
										for ( SearchResult sr: srs ){
											
											System.out.println( "        " + sr.getProperty( SearchResult.PR_NAME ));
										}
									}
								}
								
								public void
								completed()
								{
									System.out.println( "received completed" );
									
									complete[0] = true;
								}
							});
					
					while( !complete[0] ){
						
						Thread.sleep(1000);
						
						System.out.println( "waiting for results" );
					}
					*/
					
					
					Search s = si.createSearch(	providers, properties, null );
					
					while( !s.isComplete()){
						
						Thread.sleep(1000);
						
						SearchProviderResults[] results = s.getResults();
						
						if ( results.length > 0 ){
						
							System.out.println( "Got results: " + results.length );
							
							for ( SearchProviderResults result: results ){
								
								System.out.println( "    " + result.getProvider().getProperty( SearchProvider.PR_NAME ) + ": comp=" + result.isComplete() + ", error=" + result.getError());
								
								SearchResult[] srs = result.getResults();
								
								for ( SearchResult sr: srs ){
									
									System.out.println( "        " + sr.getProperty( SearchResult.PR_NAME ));
								}
							}
						}
					}
					
					break;
				}
			}catch( Throwable e){
				
				e.printStackTrace();
			}
		}
	}
	
	private void
	testLinks()
	{
		plugin_interface.getDownloadManager().addListener(
			new DownloadManagerListener()
			{
				public void
				downloadAdded(
					Download	download )
				{
					DiskManagerFileInfo[]	info = download.getDiskManagerFileInfo();
					
					for (int i=0;i<info.length;i++){
						
						info[i].setLink( new File( "C:\\temp" ));
					}
				}
				
				public void
				downloadRemoved(
					Download	download )
				{
					
				}
			});
	}
	
	private void
	testDDB()
	{
		try{
			DistributedDatabase	db = plugin_interface.getDistributedDatabase();
			
			DistributedDatabaseKey	key = db.createKey( new byte[]{ 4,7,1,2,5,8 });

			boolean	do_write	= false;
			
			if ( do_write ){
				
				DistributedDatabaseValue[] values = new DistributedDatabaseValue[500];
				
				for (int i=0;i<values.length;i++){
					
					byte[]	val = new byte[20];
					
					Arrays.fill( val, (byte)i );
					
					values[i] = db.createValue( val );
				}
				
				
				db.write(
					new DistributedDatabaseListener()
					{
						public void
						event(
							DistributedDatabaseEvent		event )
						{
							System.out.println( "Event:" + event.getType());
							
							if ( event.getType() == DistributedDatabaseEvent.ET_VALUE_WRITTEN ){
								
								try{
									System.out.println( 
											"    write - key = " + 
											ByteFormatter.encodeString((byte[])event.getKey().getKey()) + 
											", val = " + ByteFormatter.encodeString((byte[]) event.getValue().getValue(byte[].class)));
									
								}catch( Throwable e ){
									
									e.printStackTrace();
								}
							}
						}
					},
					key,
					values );
			}else{
				
				db.read(
						new DistributedDatabaseListener()
						{
							public void
							event(
								DistributedDatabaseEvent		event )
							{
								System.out.println( "Event:" + event.getType());
								
								if ( event.getType() == DistributedDatabaseEvent.ET_VALUE_READ ){
									
									try{
										System.out.println( 
												"    read - key = " + 
												ByteFormatter.encodeString((byte[])event.getKey().getKey()) + 
												", val = " + ByteFormatter.encodeString((byte[]) event.getValue().getValue(byte[].class)));
										
									}catch( Throwable e ){
										
										e.printStackTrace();
									}
								}
							}
						},
						key,
						60000 );			
			}
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	private void
	taTest()
	{
		try{
			
			final TorrentAttribute ta = plugin_interface.getTorrentManager().getAttribute(TorrentAttribute.TA_CATEGORY);
			
			ta.addTorrentAttributeListener(
				new TorrentAttributeListener()
				{
					public void
					event(
						TorrentAttributeEvent	ev )
					{
						System.out.println( "ev: " + ev.getType() + ", " + ev.getData());
						
						if ( ev.getType() == TorrentAttributeEvent.ET_ATTRIBUTE_VALUE_ADDED ){
							
							if ( "plop".equals( ev.getData())){
								
								ta.removeDefinedValue( "plop" );
							}
						}
					}
				});
			
			ta.addDefinedValue( "wibble" );
					
			
			plugin_interface.getDownloadManager().addListener(
				new DownloadManagerListener()
				{
					public void
					downloadAdded(
						Download	download )
					{
						try{
							download.setAttribute( ta, "wibble" );
							
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}
					
					public void
					downloadRemoved(
						Download	download )
					{
						
					}
				});
				
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	public static void
	main(
		String[]	args )
	{
		System.setProperty( "azureus.dynamic.plugins", "org.gudy.azureus2.pluginsimpl.local.test.Test" );
		
		org.gudy.azureus2.ui.swt.Main.main( args );
	}
}
