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
package com.victorfranca.duedate.api.server;

import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.victorfranca.duedate.api.datasource.CalendarDataSource;
import com.victorfranca.duedate.api.datasource.CalendarDataSourceException;

@RestController
class HeyDueDateCalendarsController {

	@Value("${calendar-datasource.type}")
	private String dataSourceType;

	@Autowired
	private BeanFactory beanFactory;

	@GetMapping(value = "/calendar")
	public List<String> getDueDateWithLog() {

		try {
			CalendarDataSource calendarDataSource = beanFactory.getBean(dataSourceType, CalendarDataSource.class);
			return calendarDataSource.getCalendars();
		} catch (CalendarDataSourceException e) {
			throw new RuntimeException("Internal Error", e);
		}

	}

}