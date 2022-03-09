package com.victorfranca.duedate.rest.calculator;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.victorfranca.duedate.calculator.DueDateCalculator;
import com.victorfranca.duedate.calendar.Calendar;
import com.victorfranca.duedate.calendar.CalendarBlock;
import com.victorfranca.duedate.calendar.provider.CalendarProvider;

@RestController
@RequestMapping("/duedate")
class DueDateCalculatorController {

	private static final String LOCATION_ID_1 = "LOCATION_ID_1";

	@GetMapping(value = "/{startDateTime}/{slaInMinutes}")
	public LocalDateTime findById(
			@PathVariable("startDateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime,
			@PathVariable("slaInMinutes") Integer slaInMinutes) {

		DueDateCalculator dueDateCalculator = new DueDateCalculator(new CalendarProvider() {
			@Override
			public Calendar getCalendar() {

				Calendar calendar = new Calendar();
				CalendarBlock calendarBlock1 = new CalendarBlock(LOCATION_ID_1,
						startDateTime.withHour(3).withMinute(0).withSecond(0).withNano(0),
						startDateTime.withHour(6).withMinute(0).withSecond(0).withNano(0));

				calendar.addCalendarBlock(calendarBlock1);

				return calendar;
			}
		});

		LocalDateTime dueDateTime = dueDateCalculator.calculateDueDate(startDateTime, slaInMinutes);

		return dueDateTime;

	}

}
