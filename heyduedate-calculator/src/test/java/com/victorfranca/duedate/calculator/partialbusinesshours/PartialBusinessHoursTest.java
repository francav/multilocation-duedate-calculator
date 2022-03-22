/*
 * Copyright WKS Power Limited under one or more contributor license agreements. 
 * See the LICENSE file distributed with this work for additional information regarding 
 * copyright ownership. WKS Power licenses this file to you under the 
 * GNU General Public License v3.0; you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/gpl-3.0.en.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.victorfranca.duedate.calculator.partialbusinesshours;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.victorfranca.duedate.calculator.DueDateCalculator;
import com.victorfranca.duedate.calendar.Calendar;
import com.victorfranca.duedate.calendar.LocationRegularBusinessHours;

public class PartialBusinessHoursTest {

	private Calendar calendar;
	private DueDateCalculator dueDateCalculator;

	private static final String LOCATION_ID_1 = "LOCATION_ID_1";
	private static final int START_HOUR_1 = 8;
	private static final int END_HOUR_1 = 12;

	private static final int PARTIAL_START_HOUR_1 = 12;
	private static final int PARTIAL_END_HOUR_1 = 14;

	@Test
	public void partialBusinessHoursTest() {
		// Given
		calendar = new Calendar();
		dueDateCalculator = new DueDateCalculator();

		Map<LocalDate, PartialBusinessHour> partialBusinessHoursMap = new LinkedHashMap<>();
		partialBusinessHoursMap.put(LocalDate.of(2022, 1, 1), PartialBusinessHour.builder()
				.startHour(PARTIAL_START_HOUR_1).startMinute(0).endHour(PARTIAL_END_HOUR_1).endMinute(0).build());

		calendar.setRegularBusinessHours(List.of(
				LocationRegularBusinessHours.builder().location(LOCATION_ID_1).startHour(START_HOUR_1).startMinute(0)
						.endHour(END_HOUR_1).endMinute(0).partialBusinessHoursMap(partialBusinessHoursMap).build()));

		// When
		int slaInMinutes = 60 * 1;
		LocalDateTime startDateTime = LocalDateTime.of(2022, 1, 1, 00, 00);

		// Then
		assertEquals(LocalDateTime.of(2022, 1, 1, 13, 00),
				dueDateCalculator.calculateDueDate(calendar, startDateTime, slaInMinutes).getDueDateTime());
	}

}