/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.views.piece;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.AbstractIView;
import org.gudy.azureus2.ui.swt.views.utils.CoordinateTransform;

import com.aelitis.azureus.core.peermanager.piecepicker.PiecePicker;


/**
 * @author The8472
 * @created Jun 28, 2007
 *
 */
public class PieceDistributionView
	extends AbstractIView
{
	private Composite comp;
	private Canvas pieceDistCanvas;
	private GC pieceDistGC;
	private Image img;
	private DownloadManager dlm;
	private boolean initialized = false;

	public PieceDistributionView() {

	}

	public void dataSourceChanged(Object newDataSource) {
		if (newDataSource instanceof DownloadManager) {
			dlm = (DownloadManager)newDataSource;
			updateDistribution();
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.views.AbstractIView#getData()
	 */
	public String getData() {
		return "PiecesView.DistributionView.title";
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.views.AbstractIView#initialize(org.eclipse.swt.widgets.Composite)
	 */
	public void initialize(Composite parent) {
		comp = new Composite(parent,SWT.NONE);
		createPieceDistPanel();
		initialized = true;
		updateDistribution();
	}

	private void createPieceDistPanel() {
		comp.setLayout(new FillLayout());
		//pieceDistComposite = new Composite(parent, SWT.NONE);
		pieceDistCanvas = new Canvas(comp,SWT.NONE);
		pieceDistGC = new GC(pieceDistCanvas);
	}

	public void updateDistribution() {
		if(!initialized || dlm == null || dlm.getPeerManager() == null || dlm.getPeerManager().getPiecePicker() == null)
			return;
		Rectangle rect = pieceDistCanvas.getBounds();
		if(rect.height <= 0 || rect.width <= 0)
			return;
		if(img != null)
			img.dispose();
	
		PiecePicker picker = dlm.getPeerManager().getPiecePicker();
		
		final int seeds = dlm.getPeerManager().getNbSeeds();
		final int connected = dlm.getPeerManager().getNbPeers() + seeds;
		final int upperBound = 1+(1<<(int)Math.ceil(Math.log(connected+0.0)/Math.log(2.0)));
		//System.out.println("conn:"+connected+" bound:"+upperBound);
		final int minAvail = (int)picker.getMinAvailability();
		final int maxAvail = picker.getMaxAvailability();
		final int nbPieces = picker.getNumberOfPieces();
		final int[] availabilties = picker.getAvailability();
		final DiskManagerPiece[] dmPieces = dlm.getDiskManager().getPieces();
		final PEPiece[] pePieces = dlm.getPeerManager().getPieces();
		final int[] piecesPerAvailability = new int[upperBound];
		final int[] ourPiecesPerAvailability = new int[upperBound];
		final boolean[] downloading = new boolean[upperBound];
		
		int avlPeak = 0;
		int avlPeakIdx = -1;
		
		for(int i=0;i<nbPieces;i++)
		{
			if(availabilties[i] >= upperBound) 
				return; // availability and peer lists are OOS, just wait for the next round
			final int newPeak;
			if(avlPeak < (newPeak = ++piecesPerAvailability[availabilties[i]]))
			{
				 avlPeak = newPeak;
				 avlPeakIdx = availabilties[i];
			}
			if(dmPieces[i].isDone())
				++ourPiecesPerAvailability[availabilties[i]];
			if(pePieces[i] != null)
				downloading[availabilties[i]] = true;
		}
		
		img = new Image(pieceDistGC.getDevice(),pieceDistCanvas.getBounds());
		
		GC gc = new GC(img);

		try
		{
			int stepWidthX = (int)Math.floor(rect.width/upperBound);
			int barGap = 2;
			int barWidth = stepWidthX-barGap;
			int barFillingWidth = barWidth-1;
			int stepWidthY = (int)Math.floor(rect.height/avlPeak);
			int offsetY = rect.height;
			
			gc.setForeground(Colors.green);
			for(int i=0;i<connected;i++)
			{
				Color curColor;
				if(i<seeds)
					curColor = Colors.green;
				else
					curColor = Colors.blues[Colors.BLUES_DARKEST];
				if(i==minAvail)
					curColor = Colors.blue;
				gc.setBackground(curColor);
				gc.setForeground(curColor);
				
				if(piecesPerAvailability[i] == 0)
				{
					gc.setLineWidth(3);
					gc.drawLine(stepWidthX*i, offsetY-1, stepWidthX*i+barWidth, offsetY-1);
				} else
				{
					gc.setLineWidth(1);
					if(downloading[i])
						gc.setLineStyle(SWT.LINE_DASH);
					gc.fillRectangle(stepWidthX*i+1, offsetY, barFillingWidth, stepWidthY*ourPiecesPerAvailability[i]*-1);
					gc.drawRectangle(stepWidthX*i, offsetY, barWidth, (stepWidthY*piecesPerAvailability[i]+1)*-1);
				}
				
				gc.setLineStyle(SWT.LINE_SOLID);
			}
			
			gc.setLineWidth(1);			
			gc.setTransform(null);
			
			CoordinateTransform t = new CoordinateTransform(rect);
			
			t.shiftExternal(rect.width,0);
			t.scale(-1.0, 1.0);
			
			String[] boxContent = new String[] {
				MessageText.getString("PiecesView.DistributionView.SeedAvl"),
				MessageText.getString("PiecesView.DistributionView.PeerAvl"),
				MessageText.getString("PiecesView.DistributionView.RarestAvl",new String[] {piecesPerAvailability[minAvail]+"",minAvail+""}),
				MessageText.getString("PiecesView.DistributionView.weHave"),
				MessageText.getString("PiecesView.DistributionView.weDownload")				
				};

			int charWidth = gc.getFontMetrics().getAverageCharWidth();
			int charHeight = gc.getFontMetrics().getHeight();
			int maxBoxOffsetY = charHeight+2;
			int maxBoxWidth = 0;
			int maxBoxOffsetX = 0;
			for(int i=0;i<boxContent.length;i++)
				maxBoxWidth = Math.max(maxBoxWidth, boxContent[i].length());
			
			maxBoxOffsetX = (maxBoxWidth+5) * charWidth;
			maxBoxWidth = ++maxBoxWidth * charWidth;
			

			gc.setForeground(Colors.green);
			gc.setBackground(Colors.background);
			gc.drawRectangle(t.x(maxBoxOffsetX),t.y(maxBoxOffsetY),maxBoxWidth,charHeight);
			gc.drawString(boxContent[0],t.x(maxBoxOffsetX-5),t.y(maxBoxOffsetY),true);
			
			gc.setForeground(Colors.blues[Colors.BLUES_DARKEST]);
			gc.drawRectangle(t.x(maxBoxOffsetX),t.y(maxBoxOffsetY*2),maxBoxWidth,charHeight);
			gc.drawString(boxContent[1],t.x(maxBoxOffsetX-5),t.y(maxBoxOffsetY*2),true);

			gc.setForeground(Colors.blue);
			gc.drawRectangle(t.x(maxBoxOffsetX),t.y(maxBoxOffsetY*3),maxBoxWidth,charHeight);
			gc.drawString(boxContent[2],t.x(maxBoxOffsetX-5),t.y(maxBoxOffsetY*3),true);
			
			gc.setForeground(Colors.black);
			gc.setBackground(Colors.black);
			gc.drawRectangle(t.x(maxBoxOffsetX),t.y(maxBoxOffsetY*4),maxBoxWidth,charHeight);
			gc.fillRectangle(t.x(maxBoxOffsetX),t.y(maxBoxOffsetY*4),maxBoxWidth/2,charHeight);
			gc.setForeground(Colors.grey);
			gc.setBackground(Colors.background);
			gc.drawString(boxContent[3],t.x(maxBoxOffsetX-5),t.y(maxBoxOffsetY*4),true);
			
			gc.setForeground(Colors.black);
			gc.setLineStyle(SWT.LINE_DASH);
			gc.drawRectangle(t.x(maxBoxOffsetX),t.y(maxBoxOffsetY*5),maxBoxWidth,charHeight);
			gc.drawString(boxContent[4],t.x(maxBoxOffsetX-5),t.y(maxBoxOffsetY*5),true);
			
			gc.setLineStyle(SWT.LINE_SOLID);
		
		} finally
		{
			gc.dispose();
		}
	
		
	}

	public void refresh() {
		super.refresh();
		if(!initialized)
			return;
		updateDistribution();
		if(img != null && !img.isDisposed())
			pieceDistGC.drawImage(img, 0, 0);

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.views.AbstractIView#getComposite()
	 */
	public Composite getComposite() {
		return comp;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.views.AbstractIView#delete()
	 */
	public void delete() {
		if(!initialized)
			return;
		initialized = false;
		super.delete();
		comp.dispose();
		pieceDistCanvas.dispose();
		pieceDistGC.dispose();
		if(img != null && !img.isDisposed())
			img.dispose();
	}
}
