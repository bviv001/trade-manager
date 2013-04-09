/* ===========================================================
 * TradeManager : a application to trade strategies for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2011-2011, by Simon Allen and Contributors.
 *
 * Project Info:  org.trade
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Java is a trademark or registered trademark of Oracle, Inc.
 * in the United States and other countries.]
 *
 * (C) Copyright 2011-2011, by Simon Allen and Contributors.
 *
 * Original Author:  Simon Allen;
 * Contributor(s):   -;
 *
 * Changes
 * -------
 *
 */
package org.trade.broker;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import junit.framework.TestCase;

import org.jfree.data.DataUtilities;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trade.core.dao.Aspect;
import org.trade.core.dao.Aspects;
import org.trade.core.factory.ClassFactory;
import org.trade.core.properties.ConfigProperties;
import org.trade.core.util.TradingCalendar;
import org.trade.persistent.PersistentModel;
import org.trade.persistent.PersistentModelException;
import org.trade.persistent.dao.Candle;
import org.trade.persistent.dao.Contract;
import org.trade.persistent.dao.Strategy;
import org.trade.persistent.dao.Trade;
import org.trade.persistent.dao.TradeOrder;
import org.trade.persistent.dao.Tradestrategy;
import org.trade.persistent.dao.Tradingday;
import org.trade.persistent.dao.Tradingdays;
import org.trade.strategy.data.CandleDataset;
import org.trade.strategy.data.CandleSeries;
import org.trade.strategy.data.IndicatorSeries;
import org.trade.ui.TradeAppLoadConfig;
import org.trade.ui.base.BasePanel;

/**
 * Some tests for the {@link DataUtilities} class.
 * 
 * @author Simon Allen
 * @version $Revision: 1.0 $
 */
public class TWSBrokerModelTest extends TestCase implements
		BrokerChangeListener {

	private final static Logger _log = LoggerFactory
			.getLogger(TWSBrokerModelTest.class);

	private BrokerModel brokerManagerModel;
	private PersistentModel m_tradePersistentModel = null;
	private Integer clientId;
	private Integer port = null;
	private String host = null;
	private int grandTotal = 0;
	private long last6SubmittedTime = 0;
	private Integer backTestBarSize = 0;
	private long startTime = 0;
	private final static String _broker = BrokerModel._broker;
	private static final ConcurrentHashMap<Integer, Tradestrategy> _indicatorTradestrategy = new ConcurrentHashMap<Integer, Tradestrategy>();

	/**
	 * Method setUp.
	 * 
	 * @throws Exception
	 */
	protected void setUp() throws Exception {
		try {
			TradeAppLoadConfig.loadAppProperties();
			this.m_tradePersistentModel = (PersistentModel) ClassFactory
					.getServiceForInterface(PersistentModel._persistentModel,
							this);
			this.brokerManagerModel = (BrokerModel) ClassFactory
					.getServiceForInterface(_broker, this);
			brokerManagerModel.addMessageListener(this);

			clientId = ConfigProperties.getPropAsInt("trade.tws.clientId");
			port = new Integer(
					ConfigProperties.getPropAsString("trade.tws.port"));
			host = ConfigProperties.getPropAsString("trade.tws.host");
			this.brokerManagerModel.onConnect(host, port, clientId);
			backTestBarSize = ConfigProperties
					.getPropAsInt("trade.backtest.barSize");
			try {
				int i = 0;
				do {
					i++;
					Thread.sleep(1000);
					if (i > 10)
						break;
				} while (!this.brokerManagerModel.isConnected());
				// assertTrue("Connected to TWS", m_brokerModel.isConnected());

			} catch (InterruptedException e) {
				_log.info("Thread interrupt: " + e.getMessage());
			}

		} catch (Exception e) {
			TestCase.fail("Error on setup " + e.getMessage());
		}
	}

	/**
	 * Method tearDown.
	 * 
	 * @throws Exception
	 */
	protected void tearDown() throws Exception {
		this.brokerManagerModel.disconnect();
	}

	@Test
	public void testOneSymbolTodayOnBrokerData() {
		Tradingdays tradingdays = new Tradingdays();
		try {
			if (this.brokerManagerModel.isConnected()) {

				String fileName = "trade/test/org/trade/broker/OneSymbolToday.csv";
				Date tradingDay = new Date();
				tradingDay = TradingCalendar.getPrevTradingDay(tradingDay);

				Tradingday tradingday = new Tradingday(
						TradingCalendar.getBusinessDayStart(tradingDay),
						TradingCalendar.getBusinessDayEnd(tradingDay));

				tradingdays.populateDataFromFile(fileName, tradingday);

				for (Tradingday item : tradingdays.getTradingdays()) {
					m_tradePersistentModel.persistTradingday(item);
				}

				doInBackground(tradingdays);
			}
		} catch (Exception ex) {
			TestCase.fail("Error testOneSymbolOnBrokerData Msg: "
					+ ex.getMessage());
		} finally {
			deleteData();
		}
	}

	@Test
	public void testMarch2013OnBrokerData() {
		Tradingdays tradingdays = new Tradingdays();
		try {
			if (this.brokerManagerModel.isConnected()) {

				String fileName = "trade/test/org/trade/broker/GappersMarch2013.csv";
				Date tradingDay = new Date();
				tradingDay = TradingCalendar.getPrevTradingDay(tradingDay);

				Tradingday tradingday = new Tradingday(
						TradingCalendar.getBusinessDayStart(tradingDay),
						TradingCalendar.getBusinessDayEnd(tradingDay));

				tradingdays.populateDataFromFile(fileName, tradingday);

				for (Tradingday item : tradingdays.getTradingdays()) {
					m_tradePersistentModel.persistTradingday(item);
				}

				doInBackground(tradingdays);
			}
		} catch (Exception ex) {
			TestCase.fail("Error testMarch2013OnBrokerData Msg: "
					+ ex.getMessage());
		} finally {
			deleteData();
		}
	}

	@Test
	public void testOneSymbolTwoMths2013OnBrokerData() {
		Tradingdays tradingdays = new Tradingdays();
		try {
			if (this.brokerManagerModel.isConnected()) {

				String fileName = "trade/test/org/trade/broker/OneSymbolTwoMths2013.csv";
				Date tradingDay = new Date();
				tradingDay = TradingCalendar.getPrevTradingDay(tradingDay);

				Tradingday tradingday = new Tradingday(
						TradingCalendar.getBusinessDayStart(tradingDay),
						TradingCalendar.getBusinessDayEnd(tradingDay));

				tradingdays.populateDataFromFile(fileName, tradingday);
				/*
				 * Set the chart days to one day so no over lap.
				 */
//				for (Tradingday item : tradingdays.getTradingdays()) {
//					for (Tradestrategy tradestrategy : item
//							.getTradestrategies()) {
//						tradestrategy.setChartDays(1);
//					}
//				}

				for (Tradingday item : tradingdays.getTradingdays()) {
					m_tradePersistentModel.persistTradingday(item);
				}

				doInBackground(tradingdays);
			}
		} catch (Exception ex) {
			TestCase.fail("Error testOneSymbolMarch2013OnBrokerData Msg: "
					+ ex.getMessage());
		} finally {
			deleteData();
		}
	}

	private void doInBackground(Tradingdays tradingdays) {
		String message = null;
		int totalSumbitted = 0;
		int reSumbittedAt = 20;
		ConcurrentHashMap<Integer, Tradingday> runningContractRequests = new ConcurrentHashMap<Integer, Tradingday>();
		this.grandTotal = 0;

		try {

			for (Tradingday tradingday : tradingdays.getTradingdays()) {
				this.grandTotal = this.getGrandTotal()
						+ tradingday.getTradestrategies().size();
			}
			this.startTime = System.currentTimeMillis();
			this.last6SubmittedTime = startTime;
			// Initialize the progress bar

			Collections.sort(tradingdays.getTradingdays(),
					Tradingday.DATE_ORDER_ASC);

			for (Tradingday tradingday : tradingdays.getTradingdays()) {

				totalSumbitted = processTradingday(
						getTradingdayToProcess(tradingday,
								runningContractRequests), totalSumbitted);
				/*
				 * Every reSumbittedAt value submitted contracts try to run any
				 * that could not be run due to a conflict. Run then in asc date
				 * order value.
				 */
				if (totalSumbitted > reSumbittedAt) {
					reSumbittedAt = totalSumbitted + reSumbittedAt;
					totalSumbitted = reProcessTradingdays(tradingdays,
							runningContractRequests, totalSumbitted);
				}
			}
			/*
			 * If we are getting data for back testing and the backTestBarSize
			 * is set. Then get the candles for the tradestrategy tradingday
			 * with the new bar size setting. The backTestBroker will use these
			 * candles to build up the candle on the Tradestrategy/BarSize.
			 */
			if (backTestBarSize > 0
					&& this.brokerManagerModel.isBrokerDataOnly()) {
				Collections.sort(tradingdays.getTradingdays(),
						Tradingday.DATE_ORDER_ASC);
				for (Tradingday itemTradingday : tradingdays.getTradingdays()) {
					Date today = new Date();
					if (!(TradingCalendar.isMarketHours(
							itemTradingday.getOpen(),
							itemTradingday.getClose(), today) && TradingCalendar
							.sameDay(itemTradingday.getOpen(), today))) {
						if (itemTradingday.getTradestrategies().isEmpty())
							continue;
						Tradingday tradingday = (Tradingday) itemTradingday
								.clone();
						for (Tradestrategy itemTradestrategy : itemTradingday
								.getTradestrategies()) {
							if (backTestBarSize < itemTradestrategy
									.getBarSize()) {
								Tradestrategy tradestrategy = (Tradestrategy) itemTradestrategy
										.clone();
								tradestrategy.setBarSize(backTestBarSize);
								tradestrategy.setChartDays(1);
								tradestrategy
										.setIdTradeStrategy(this.brokerManagerModel
												.getNextRequestId());
								tradingday.addTradestrategy(tradestrategy);
								this.grandTotal++;
							}
						}
						totalSumbitted = processTradingday(
								getTradingdayToProcess(tradingday,
										runningContractRequests),
								totalSumbitted);
					}
				}
			}

			/*
			 * Every reSumbittedAt value submitted contracts try to run any that
			 * could not be run due to a conflict. Run then in asc date order
			 * value.
			 */

			totalSumbitted = reProcessTradingdays(tradingdays,
					runningContractRequests, totalSumbitted);

		} catch (InterruptedException ex) {
			// Do nothing
			_log.error("doInBackground interupted Msg: ", ex.getMessage());
		} catch (Exception ex) {
			_log.error("Error getting history data Msg: ", ex.getMessage());

		} finally {
			synchronized (this.brokerManagerModel.getHistoricalData()) {
				while ((this.brokerManagerModel.getHistoricalData().size() > 0)) {
					int percent = (int) (((double) (getGrandTotal() - this.brokerManagerModel
							.getHistoricalData().size()) / getGrandTotal()) * 100d);
					_log.info("Percent complete: " + percent);
					try {
						this.brokerManagerModel.getHistoricalData().wait();
					} catch (InterruptedException ex) {
						// Do nothing
						_log.error("doInBackground finally interupted Msg: ",
								ex.getMessage());
					}
				}
			}

			message = "Completed Historical data total contracts processed: "
					+ totalSumbitted + " in : "
					+ ((System.currentTimeMillis() - this.startTime) / 1000)
					+ " Seconds.";
			_log.info(message);
		}
	}

	private int processTradingday(Tradingday tradingday, int totalSumbitted)
			throws BrokerModelException, InterruptedException,
			CloneNotSupportedException, PersistentModelException {

		if (tradingday.getTradestrategies().isEmpty())
			return totalSumbitted;
		/*
		 * Remove those that are not running as these do not need to be shared.
		 */
		for (Tradestrategy tradestrategy : _indicatorTradestrategy.values()) {
			if (!brokerManagerModel.isRealtimeBarsRunning(tradestrategy)
					&& !brokerManagerModel
							.isHistoricalDataRunning(tradestrategy)) {
				_indicatorTradestrategy.remove(tradestrategy
						.getIdTradeStrategy());
			}
		}
		Contract prevContract = null;
		Integer prevBarSize = null;
		Integer prevChartDays = null;
		for (Tradestrategy tradestrategy : tradingday.getTradestrategies()) {

			if (null == prevContract) {
				prevContract = tradestrategy.getContract();
				prevBarSize = tradestrategy.getBarSize();
				prevChartDays = tradestrategy.getChartDays();
				_indicatorTradestrategy.put(tradestrategy.getIdTradeStrategy(),
						tradestrategy);
			}
			/*
			 * Refresh the data set container as these may have changed.
			 */
			tradestrategy.setDatasetContainer(null);

			if (!brokerManagerModel.isRealtimeBarsRunning(tradestrategy)) {

				/*
				 * Fire all the requests to TWS to get chart data After data has
				 * been retrieved save the data Only allow a maximum of 60
				 * requests in a 10min period to avoid TWS pacing errors
				 */

				if (!prevContract.equals(tradestrategy.getContract())) {
					totalSumbitted = submitBrokerRequest(prevContract,
							tradingday.getClose(), prevBarSize, prevChartDays,
							totalSumbitted);
					prevContract = tradestrategy.getContract();
					prevBarSize = tradestrategy.getBarSize();
					prevChartDays = tradestrategy.getChartDays();
					_indicatorTradestrategy.put(
							tradestrategy.getIdTradeStrategy(), tradestrategy);
				}
			}
		}
		if (null != prevContract) {
			totalSumbitted = submitBrokerRequest(prevContract,
					tradingday.getClose(), prevBarSize, prevChartDays,
					totalSumbitted);
		}
		/*
		 * Now process the indicators that are candle based.
		 */
		for (Tradestrategy tradestrategy : tradingday.getTradestrategies()) {
			CandleDataset candleDataset = (CandleDataset) tradestrategy
					.getDatasetContainer().getIndicatorByType(
							IndicatorSeries.CandleSeries);

			if (null != candleDataset) {
				for (int seriesIndex = 0; seriesIndex < candleDataset
						.getSeriesCount(); seriesIndex++) {

					CandleSeries series = candleDataset.getSeries(seriesIndex);
					Tradestrategy indicatorTradestrategy = getIndicatorTradestrategy(
							tradestrategy, series);
					candleDataset.setSeries(seriesIndex, indicatorTradestrategy
							.getDatasetContainer().getBaseCandleSeries());
					if (!_indicatorTradestrategy
							.containsKey(indicatorTradestrategy
									.getIdTradeStrategy())) {
						if (brokerManagerModel.isConnected()
								|| brokerManagerModel.isBrokerDataOnly()) {
							_indicatorTradestrategy
									.put(indicatorTradestrategy
											.getIdTradeStrategy(),
											indicatorTradestrategy);
							this.grandTotal++;
							indicatorTradestrategy.getContract()
									.addTradestrategy(indicatorTradestrategy);
							totalSumbitted = submitBrokerRequest(
									indicatorTradestrategy.getContract(),
									tradingday.getClose(),
									indicatorTradestrategy.getBarSize(),
									indicatorTradestrategy.getChartDays(),
									totalSumbitted);
						}
					}
				}
			}
		}
		return totalSumbitted;
	}

	/**
	 * Method submitBrokerRequest.
	 * 
	 * @param tradestrategy
	 *            Tradestrategy
	 * @param totalSumbitted
	 *            int
	 * @return int
	 * @throws InterruptedException
	 * @throws BrokerModelException
	 */
	private int submitBrokerRequest(Contract contract, Date endDate,
			Integer barSize, Integer chartDays, int totalSumbitted)
			throws InterruptedException, BrokerModelException {

		if (brokerManagerModel.isHistoricalDataRunning(contract)) {
			return totalSumbitted;
		}
		_log.info("submitBrokerRequest: " + contract.getSymbol() + " endDate: "
				+ endDate);

		totalSumbitted++;
		hasSubmittedInSeconds(totalSumbitted);
		brokerManagerModel.onBrokerData(contract, endDate, barSize, chartDays);

		_log.info("Total: " + this.grandTotal + " totalSumbitted: "
				+ totalSumbitted);
		/*
		 * Need to slow things down as limit is 60 including real time bars
		 * requests. When connected to TWS. Note only TWSManager return true for
		 * connected.
		 */
		if (((Math.floor(totalSumbitted / 58d) == (totalSumbitted / 58d)) && (totalSumbitted > 0))
				&& brokerManagerModel.isConnected()) {
			int waitTime = 0;
			while ((waitTime < 601000)) {
				String message = "Please wait " + (10 - (waitTime / 60000))
						+ " minutes as there are more than 60 data requests.";
				waitTime = waitTime + 1000;
				Thread.sleep(1000);
				_log.info(message);
			}
		}

		/*
		 * The SwingWorker has a maximum of 10 threads to run and this process
		 * uses one so we have 9 left for the BrokerWorkers. So wait while the
		 * BrokerWorkers threads complete.
		 */
		synchronized (this.brokerManagerModel.getHistoricalData()) {
			while ((this.brokerManagerModel.getHistoricalData().size() > 8)) {
				this.brokerManagerModel.getHistoricalData().wait();
			}
		}

		int percent = (int) (((double) (totalSumbitted - this.brokerManagerModel
				.getHistoricalData().size()) / this.getGrandTotal()) * 100d);
		_log.info("Percent complete: " + percent);
		return totalSumbitted;
	}

	/**
	 * Method getIndicatorTradestrategy. For any child indicators that are
	 * candle based create a Tradestrategy that will get the data. If this
	 * tradestrategy already exist share this with any other tradestrategy that
	 * requires this.
	 * 
	 * @param tradestrategy
	 *            Tradestrategy
	 * @param series
	 *            CandleSeries
	 * @return Tradestrategy
	 * @throws BrokerModelException
	 * @throws PersistentModelException
	 * @throws CloneNotSupportedException
	 */
	private Tradestrategy getIndicatorTradestrategy(
			Tradestrategy tradestrategy, CandleSeries series)
			throws BrokerModelException, PersistentModelException,
			CloneNotSupportedException {

		Tradestrategy indicatorTradestrategy = null;
		for (Tradestrategy indicator : _indicatorTradestrategy.values()) {
			if (indicator.getContract().equals(series.getContract())
					&& indicator.getTradingday().equals(
							tradestrategy.getTradingday())
					&& indicator.getBarSize()
							.equals(tradestrategy.getBarSize())
					&& indicator.getChartDays().equals(
							tradestrategy.getChartDays())
					&& indicator.getPortfolio().equals(
							tradestrategy.getPortfolio())) {
				indicatorTradestrategy = indicator;
				break;
			}
		}
		if (null == indicatorTradestrategy) {
			Contract contract = series.getContract();
			if (null == series.getContract().getIdContract()) {
				contract = m_tradePersistentModel.findContractByUniqueKey(
						series.getContract().getSecType(), series.getContract()
								.getSymbol(), series.getContract()
								.getExchange(), series.getContract()
								.getCurrency(), series.getContract()
								.getExpiry());
				if (null == contract) {
					contract = (Contract) m_tradePersistentModel
							.persistAspect(series.getContract());
				}
			}
			indicatorTradestrategy = new Tradestrategy(contract,
					tradestrategy.getTradingday(), new Strategy("Indicator"),
					tradestrategy.getPortfolio(), new BigDecimal(0), null,
					null, false, tradestrategy.getChartDays(),
					tradestrategy.getBarSize());
			indicatorTradestrategy.setIdTradeStrategy(brokerManagerModel
					.getNextRequestId());
			indicatorTradestrategy.setDirty(false);
		}

		CandleSeries childSeries = indicatorTradestrategy.getDatasetContainer()
				.getBaseCandleSeries();
		childSeries.setDisplaySeries(series.getDisplaySeries());
		childSeries.setSeriesRGBColor(series.getSeriesRGBColor());
		childSeries.setSubChart(series.getSubChart());
		childSeries.setSymbol(series.getSymbol());
		childSeries.setSecType(series.getSecType());
		childSeries.setCurrency(series.getCurrency());
		childSeries.setExchange(series.getExchange());

		return indicatorTradestrategy;
	}

	/**
	 * Method hasSubmittedInSeconds. Make sure no more than six requests every 2
	 * seconds.
	 * 
	 * 162 - Historical Market Data Service error message: Historical data
	 * request pacing violation
	 * 
	 * The following conditions can cause a pacing violation:
	 * 
	 * � Making identical historical data requests within 15 seconds;
	 * 
	 * � Making six or more historical data requests for the same Contract,
	 * Exchange and Tick Type within two seconds.
	 * 
	 * Also, observe the following limitation when requesting historical data:
	 * 
	 * � Do not make more than 60 historical data requests in any ten-minute
	 * period.
	 * 
	 * @param totalSumbitted
	 *            int
	 * @throws InterruptedException
	 */
	private void hasSubmittedInSeconds(int totalSumbitted)
			throws InterruptedException {
		long currentTime = System.currentTimeMillis();
		int waitTime = 5;
		_log.info("hasSubmittedInSeconds 6 in: "
				+ ((currentTime - this.last6SubmittedTime) / 1000d));
		if (((Math.floor(totalSumbitted / 6d) == (totalSumbitted / 6d)) && (totalSumbitted > 0))
				&& this.brokerManagerModel.isConnected()) {
			while ((currentTime - this.last6SubmittedTime) < (waitTime * 1000)) {
				_log.info("hasSubmittedInSeconds will sumbit in: "
						+ (waitTime - ((currentTime - this.last6SubmittedTime) / 1000d)));
				Thread.sleep(1000);
				currentTime = System.currentTimeMillis();
			}
			this.last6SubmittedTime = currentTime;
		}
	}

	/**
	 * Method getGrandTotal.
	 * 
	 * @return int
	 */
	private int getGrandTotal() {
		return this.grandTotal;
	}

	/**
	 * Method getTradingdayToProcess. Get a tradingdays worth of strategies that
	 * have contracts with many tradestrategies. If the contract is already
	 * running add it to the set to be reprocessed later.
	 * 
	 * @param tradingday
	 *            Tradingday
	 * 
	 * @param runningContractRequests
	 *            ConcurrentHashMap<Integer, Tradingday>
	 * 
	 * @return Tradingday
	 * @throws CloneNotSupportedException
	 */

	private Tradingday getTradingdayToProcess(Tradingday tradingday,
			ConcurrentHashMap<Integer, Tradingday> runningContractRequests)
			throws CloneNotSupportedException {

		if (tradingday.getTradestrategies().isEmpty())
			return tradingday;

		Collections.sort(tradingday.getTradestrategies(),
				Tradestrategy.TRADINGDAY_CONTRACT);
		Tradingday reProcessTradingday = null;
		if (runningContractRequests.containsKey(tradingday.getIdTradingDay())) {
			reProcessTradingday = runningContractRequests.get(tradingday
					.getIdTradingDay());
		} else {
			reProcessTradingday = (Tradingday) tradingday.clone();
		}
		Tradingday toProcessTradingday = (Tradingday) tradingday.clone();
		Contract currContract = null;
		Integer currBarSize = null;
		Integer currChartDays = null;

		for (Tradestrategy tradestrategy : tradingday.getTradestrategies()) {
			if (brokerManagerModel.isHistoricalDataRunning(tradestrategy
					.getContract())) {
				if (!reProcessTradingday.existTradestrategy(tradestrategy))
					reProcessTradingday.addTradestrategy(tradestrategy);
			} else {
				toProcessTradingday.addTradestrategy(tradestrategy);
				if (tradestrategy.getContract().equals(currContract)
						&& tradestrategy.getBarSize().equals(currBarSize)
						&& tradestrategy.getChartDays().equals(currChartDays)) {
					currContract.addTradestrategy(tradestrategy);
				} else {
					currContract = tradestrategy.getContract();
					currChartDays = tradestrategy.getChartDays();
					currBarSize = tradestrategy.getBarSize();
					currContract.addTradestrategy(tradestrategy);
				}
			}
		}

		for (Tradestrategy tradestrategy : toProcessTradingday
				.getTradestrategies()) {
			if (reProcessTradingday.existTradestrategy(tradestrategy))
				reProcessTradingday.removeTradestrategy(tradestrategy);
		}
		if (reProcessTradingday.getTradestrategies().isEmpty()) {
			runningContractRequests.remove(reProcessTradingday
					.getIdTradingDay());
		}
		if (!reProcessTradingday.getTradestrategies().isEmpty()) {
			runningContractRequests.put(reProcessTradingday.getIdTradingDay(),
					reProcessTradingday);
		}
		return toProcessTradingday;
	}

	/**
	 * Method reProcessTradingdays. Every reSumbittedAt value submitted
	 * contracts try to run any that could not be run due to a conflict. Run
	 * then in asc date order value.
	 * 
	 * @param runningContractRequests
	 *            ConcurrentHashMap<Integer, Tradingday>
	 * @param totalSumbitted
	 *            int
	 * @return int
	 * @throws Exception
	 */

	private int reProcessTradingdays(Tradingdays tradingdays,
			ConcurrentHashMap<Integer, Tradingday> runningContractRequests,
			int totalSumbitted) throws Exception {

		int submitted = totalSumbitted;

		while (!runningContractRequests.isEmpty()) {

			for (Tradingday item : tradingdays.getTradingdays()) {
				for (Integer idTradeingday : runningContractRequests.keySet()) {
					Tradingday reProcessTradingday = runningContractRequests
							.get(idTradeingday);
					if (item.equals(reProcessTradingday)) {
						totalSumbitted = processTradingday(
								getTradingdayToProcess(reProcessTradingday,
										runningContractRequests),
								totalSumbitted);
						break;
					}
				}
			}
			/*
			 * If nothing submitted wait for all the processes to finish.
			 * Usually means we are submitting identical contracts.
			 */

			synchronized (this.brokerManagerModel.getHistoricalData()) {
				if (submitted == totalSumbitted) {
					while (this.brokerManagerModel.getHistoricalData().size() > 0) {
						_log.info("reProcessTradingdays Wait HistoricalDataSize: "
								+ this.brokerManagerModel.getHistoricalData()
										.size());
						this.brokerManagerModel.getHistoricalData().wait();
					}
				}
			}
			if (submitted < totalSumbitted)
				submitted = totalSumbitted;
		}
		return totalSumbitted;
	}

	private void deleteData() {

		try {

			Aspects candles = m_tradePersistentModel
					.findAspectsByClassName(Candle.class.getName());
			for (Aspect item : candles.getAspect()) {
				m_tradePersistentModel.removeAspect(item);
			}

			Aspects tradestrategies = m_tradePersistentModel
					.findAspectsByClassName(Tradestrategy.class.getName());
			for (Aspect item : tradestrategies.getAspect()) {
				m_tradePersistentModel.removeAspect(item);
			}

			Aspects contracts = m_tradePersistentModel
					.findAspectsByClassName(Contract.class.getName());
			for (Aspect item : contracts.getAspect()) {
				m_tradePersistentModel.removeAspect(item);
			}

			Aspects tradingdays = m_tradePersistentModel
					.findAspectsByClassName(Tradingday.class.getName());
			for (Aspect item : tradingdays.getAspect()) {
				m_tradePersistentModel.removeAspect(item);
			}
		} catch (Exception e) {
			TestCase.fail("Error deleteData Msg: " + e.getMessage());
		} finally {
			_log.info("All data deleted");
		}
	}

	public void connectionOpened() {
		_log.info("Connection opened");
	}

	public void connectionClosed() {
		_log.info("Connection closed");
	}

	/**
	 * Method executionDetailsEnd.
	 * 
	 * @param execDetails
	 *            ConcurrentHashMap<Integer,TradeOrder>
	 */
	public void executionDetailsEnd(
			ConcurrentHashMap<Integer, TradeOrder> execDetails) {

	}

	/**
	 * Method historicalDataComplete.
	 * 
	 * @param tradestrategy
	 *            Tradestrategy
	 */
	public void historicalDataComplete(Tradestrategy tradestrategy) {

		try {
			_log.info("Symbol: "
					+ tradestrategy.getContract().getSymbol()
					+ " Candles  saved: "
					+ m_tradePersistentModel.findCandleCount(tradestrategy
							.getTradingday().getIdTradingDay(), tradestrategy
							.getContract().getIdContract()));
		} catch (PersistentModelException ex) {
			_log.error("Error historicalDataComplete Msg: " + ex.getMessage());
		}
	}

	/**
	 * Method managedAccountsUpdated.
	 * 
	 * @param accountNumber
	 *            String
	 */
	public void managedAccountsUpdated(String accountNumber) {

	}

	/**
	 * Method fAAccountsCompleted. Notifies all registered listeners that the
	 * brokerManagerModel has received all FA Accounts information.
	 * 
	 */
	public void fAAccountsCompleted() {

	}

	/**
	 * Method updateAccountTime.
	 * 
	 * @param accountNumber
	 *            String
	 */
	public void updateAccountTime(String accountNumber) {

	}

	/**
	 * Method brokerError.
	 * 
	 * @param brokerError
	 *            BrokerModelException
	 */
	public void brokerError(BrokerModelException ex) {
		if (ex.getErrorId() == 1) {
			_log.error("Error: " + ex.getErrorCode(), ex.getMessage(), ex);
		} else if (ex.getErrorId() == 2) {
			_log.warn("Warning: " + ex.getMessage(), BasePanel.WARNING);
		} else if (ex.getErrorId() == 3) {
			_log.info("Information: " + ex.getMessage(), BasePanel.INFORMATION);
		} else {
			_log.error("Unknown Error Id Code: " + ex.getErrorCode(),
					ex.getMessage(), ex);
		}
	}

	/**
	 * Method tradeOrderFilled.
	 * 
	 * @param tradeOrder
	 *            TradeOrder
	 */
	public void tradeOrderFilled(TradeOrder tradeOrder) {

	}

	/**
	 * Method tradeOrderCancelled.
	 * 
	 * @param tradeOrder
	 *            TradeOrder
	 */
	public void tradeOrderCancelled(TradeOrder tradeOrder) {

	}

	/**
	 * Method tradeOrderStatusChanged.
	 * 
	 * @param tradeOrder
	 *            TradeOrder
	 */
	public void tradeOrderStatusChanged(TradeOrder tradeOrder) {

	}

	/**
	 * Method positionClosed.
	 * 
	 * @param trade
	 *            Trade
	 */
	public void positionClosed(Trade trade) {

	}

	/**
	 * Method openOrderEnd.
	 * 
	 * @param openOrders
	 *            ConcurrentHashMap<Integer,TradeOrder>
	 */
	public void openOrderEnd(ConcurrentHashMap<Integer, TradeOrder> openOrders) {

	}
}