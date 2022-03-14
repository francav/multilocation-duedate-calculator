package com.victorfranca.duedate.calculator;

import static com.victorfranca.duedate.calculator.Dates.addMinutes;
import static com.victorfranca.duedate.calculator.Dates.diffInMinutes;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.victorfranca.duedate.calculator.log.CalculationLog;
import com.victorfranca.duedate.calculator.log.CalculationLogBlock;
import com.victorfranca.duedate.calendar.Calendar;
import com.victorfranca.duedate.calendar.CalendarBlock;
import com.victorfranca.duedate.calendar.LocationRegularBusinessHours;
import com.victorfranca.duedate.calendar.daylightsaving.DayLightSavingVisitor;
import com.victorfranca.duedate.calendar.nonbusinesshour.NonBusinessDayVisitor;

//TODO trim seconds?
//TODO different time zones
//TODO Overlaping location times scenarios
//TODO Thread safe? Concurrency?
//TODO class and methods comments
/**
 * @author victor.franca
 * 
 */
public class DueDateCalculator {

	private List<CalendarBlock> currentDateCalendarBlocks;

	private long currentDateOnDurationInMinutes;

	private LocalDateTime currentDateStartDateTime;

	private long slaCounterInMinutes = 0;

	public DueDateCalculator() {
	}

	public CalculationLog calculateDueDateWithLog(Calendar calendar, LocalDateTime startDateTime, long slaInMinutes) {
		CalculationLog calculationLog = new CalculationLog();

		LocalDateTime dueDateTime = calculateDueDate(calendar, startDateTime, slaInMinutes, calculationLog);

		if (calculationLog != null && !calculationLog.isEmpty()) {
			calculationLog.setStartDateTime(startDateTime);
			calculationLog.setDueDateTime(dueDateTime);
			calculationLog.truncateTimeUsedBySLA();
		}

		return calculationLog;

	}

	// TODO exception when sla rolls over calendarDay onDuration
	// Exception when SLA == ZERO
	public LocalDateTime calculateDueDate(Calendar calendar, LocalDateTime startDateTime, long slaInMinutes) {
		return calculateDueDate(calendar, startDateTime, slaInMinutes, null);
	}

	private LocalDateTime calculateDueDate(Calendar calendar, LocalDateTime startDateTime, long slaInMinutes,
			CalculationLog calculationLog) {

		initCalendarBlocks(calendar, startDateTime, calculationLog);
		this.slaCounterInMinutes = slaInMinutes;
		this.currentDateStartDateTime = startDateTime;

		advanceToDueDateDay(calendar, slaInMinutes, calculationLog);

		CalendarBlock calendarBlock = getDueDateCalendarBlockAndUpdateSLACounter();
		LocalDateTime dueDateTime = addMinutes(Long.valueOf(slaCounterInMinutes).intValue(), calendarBlock.getEnd());

		return dueDateTime;
	}

	// TODO refactor: avoid side effect behavior(get block and updating global
	// variable)
	private CalendarBlock getDueDateCalendarBlockAndUpdateSLACounter() {
		Iterator<CalendarBlock> calendarBlockIterator = this.currentDateCalendarBlocks.iterator();
		CalendarBlock calendarBlock = null;
		while (slaCounterInMinutes > 0) {
			calendarBlock = calendarBlockIterator.next();

			// TODO implement calendarBlock.next() / calendarBlock.nextON()
			if (calendarBlock.isOn()) {
				if ((!currentDateStartDateTime.isAfter(calendarBlock.getEnd()))) {
					slaCounterInMinutes -= calendarBlock.getDurationInMinutes();
					if (currentDateStartDateTime.isAfter(calendarBlock.getStart())) {
						long minutesDiff = diffInMinutes(currentDateStartDateTime, calendarBlock.getStart());
						slaCounterInMinutes += minutesDiff;
					}
				}
			}
		}

		return calendarBlock;
	}

	private void advanceToDueDateDay(Calendar calendar, long slaInMinutes, CalculationLog calculationLog) {
		// TODO refactor: move to a data structure (iterator?)
		long rollingSlaMinutes = getRollingSlaMinutes(currentDateStartDateTime, slaInMinutes);
		while (rollingSlaMinutes > 0) {
			incCalendarBlocksDay(calendar, calculationLog);
			if (rollingSlaMinutes >= 0) {
				slaCounterInMinutes = rollingSlaMinutes;
				currentDateStartDateTime = this.currentDateCalendarBlocks.get(0).getStart();
			}
			rollingSlaMinutes = getRollingSlaMinutes(currentDateStartDateTime, rollingSlaMinutes);
		}
	}

	private void initCalendarBlocks(Calendar calendar, LocalDateTime startDateTime, CalculationLog calculationLog) {

		currentDateCalendarBlocks = new ArrayList<>();

		for (LocationRegularBusinessHours locationRegularBusinessHours : calendar.getRegularBusinessHours()) {

			LocalDateTime start = startDateTime.withHour(locationRegularBusinessHours.getStartHour())
					.withMinute(locationRegularBusinessHours.getStartMinute()).truncatedTo(ChronoUnit.MINUTES);

			LocalDateTime end = startDateTime.withHour(locationRegularBusinessHours.getEndHour())
					.withMinute(locationRegularBusinessHours.getEndMinute()).truncatedTo(ChronoUnit.MINUTES);

			CalendarBlock calendarBlock = new CalendarBlock(locationRegularBusinessHours.getLocation(), start, end);

			addCalendarBlock(calendar, calendarBlock, calculationLog);
		}

	}

	private void addCalendarBlock(Calendar calendar, CalendarBlock calendarBlock, CalculationLog calculationLog) {
		this.currentDateCalendarBlocks.add(calendarBlock);

		calendarBlock.accept(DayLightSavingVisitor.builder(calendar.getDayLightSavingInfoByLocation()).build());

		calendarBlock.accept(NonBusinessDayVisitor.builder(calendar.getNonBusinessDaysByLocation()).build());

		if (calendarBlock.isOn()) {
			currentDateOnDurationInMinutes += calendarBlock.getDurationInMinutes();
		}

		if (calculationLog != null) {
			// TODO replace constructor by the calendarBlock parameter
			calculationLog.add(new CalculationLogBlock(calendarBlock.getStart(), calendarBlock.getEnd(),
					calendarBlock.getDurationInMinutes(), calendarBlock.isOn(), calendarBlock.isDstAffected()));
		}

	}

	private long getRollingSlaMinutes(LocalDateTime startDateTime, long slaInMinutes) {
		return slaInMinutes - getAdaptedOnDurationInMinutes(startDateTime);
	}

	private long getAdaptedOnDurationInMinutes(LocalDateTime startDateTime) {
		long adaptedOnDurationInMinutes = currentDateOnDurationInMinutes;

		for (Iterator<CalendarBlock> iterator = currentDateCalendarBlocks.iterator(); iterator.hasNext();) {
			CalendarBlock calendarBlock = (CalendarBlock) iterator.next();
			if (calendarBlock.isOn()) {
				if (!startDateTime.isBefore(calendarBlock.getStart())) {
					adaptedOnDurationInMinutes -= diffInMinutes(startDateTime, calendarBlock.getStart())
							+ diffInMinutes(calendarBlock.getEnd(), startDateTime);
				}
				if (!startDateTime.isBefore(calendarBlock.getStart())
						&& !startDateTime.isAfter(calendarBlock.getEnd())) {
					adaptedOnDurationInMinutes += diffInMinutes(calendarBlock.getEnd(), startDateTime);
				}
			}

		}

		return adaptedOnDurationInMinutes;
	}

	private void incCalendarBlocksDay(Calendar calendar, CalculationLog calculationLog) {

		currentDateCalendarBlocks.forEach(o -> o.nextDay());

		currentDateCalendarBlocks.forEach(
				o -> o.accept(DayLightSavingVisitor.builder(calendar.getDayLightSavingInfoByLocation()).build()));

		currentDateCalendarBlocks
				.forEach(o -> o.accept(NonBusinessDayVisitor.builder(calendar.getNonBusinessDaysByLocation()).build()));

		updateOnDurationInMinutes();

		if (calculationLog != null) {
			currentDateCalendarBlocks.forEach(o -> calculationLog.add(new CalculationLogBlock(o.getStart(), o.getEnd(),
					o.getDurationInMinutes(), o.isOn(), o.isDstAffected())));
		}

	}

	private void updateOnDurationInMinutes() {
		currentDateOnDurationInMinutes = 0;
		currentDateCalendarBlocks.stream().filter(o -> o.isOn())
				.forEach(o -> currentDateOnDurationInMinutes += o.getDurationInMinutes());
	}

}
