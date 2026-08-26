#!/usr/bin/env python3
"""Generate Travel House installment step-by-step PDF report."""
from datetime import datetime
from pathlib import Path

from fpdf import FPDF

OUT = Path(r"E:\travelHouseAppium\reports\TravelHouse_Step_By_Step_Execution_Report.pdf")


class Report(FPDF):
    def header(self):
        self.set_x(self.l_margin)
        self.set_font("Helvetica", "B", 11)
        self.set_text_color(15, 76, 129)
        self.cell(0, 8, "Travel House Appium - Step-by-Step Execution Report", new_x="LMARGIN", new_y="NEXT")
        self.set_draw_color(14, 116, 144)
        self.set_line_width(0.4)
        y = self.get_y()
        self.line(self.l_margin, y, self.w - self.r_margin, y)
        self.ln(4)

    def footer(self):
        self.set_y(-14)
        self.set_font("Helvetica", "", 8)
        self.set_text_color(120, 120, 120)
        self.cell(
            0,
            8,
            f"Page {self.page_no()}/{{nb}}  |  Generated {datetime.now().strftime('%d %b %Y %H:%M')}",
            align="C",
        )

    def section(self, title):
        self.set_x(self.l_margin)
        self.ln(3)
        self.set_font("Helvetica", "B", 13)
        self.set_text_color(15, 76, 129)
        self.multi_cell(0, 7, title)
        self.set_text_color(30, 30, 30)

    def body(self, text):
        self.set_x(self.l_margin)
        self.set_font("Helvetica", "", 10)
        self.multi_cell(0, 5.2, text)
        self.ln(1)

    def bullet(self, text):
        self.set_x(self.l_margin)
        self.set_font("Helvetica", "", 10)
        self.multi_cell(0, 5.2, f"- {text}")

    def kv(self, key, value):
        self.set_x(self.l_margin)
        self.set_font("Helvetica", "B", 10)
        self.multi_cell(0, 5.5, f"{key}: {value}")

    def status_line(self, label, status):
        self.set_x(self.l_margin)
        self.set_font("Helvetica", "B", 10)
        self.set_text_color(30, 30, 30)
        self.write(6, f"{label}: ")
        color = (21, 128, 61) if "PASS" in status.upper() else (
            (180, 83, 9) if any(x in status.upper() for x in ("PARTIAL", "PROGRESS", "INCLUDED")) else (185, 28, 28)
        )
        self.set_text_color(*color)
        self.write(6, status)
        self.set_text_color(30, 30, 30)
        self.ln(7)


def build():
    pdf = Report(format="A4")
    pdf.alias_nb_pages()
    pdf.set_auto_page_break(auto=True, margin=18)
    pdf.add_page()

    pdf.set_font("Helvetica", "B", 16)
    pdf.multi_cell(0, 9, "Installment Flow - Full Step Report")
    pdf.set_font("Helvetica", "", 10)
    pdf.set_text_color(80, 80, 80)
    pdf.multi_cell(
        0,
        5,
        "London -> Islamabad | Return | Installment plans 1-6 months (date-matched) | "
        "Device Xiaomi 8797df69 | App v1.65 (14)",
    )
    pdf.set_text_color(30, 30, 30)
    pdf.ln(2)

    pdf.section("1. Run summary")
    pdf.kv("Route", "London (UK) -> Islamabad | Return trip")
    pdf.kv("Date rule", "For N-month plan: departure = today + N months, return = departure + 29 days")
    pdf.kv("Plans covered", "1, 2, 3, 4, 5, 6 month (Cheapest each) + Fastest on 6-month")
    pdf.kv("Device / App", "Xiaomi UDID 8797df69 | com.travelhouse.uk.app 1.65 / 14")
    pdf.kv("Best full green run", "12 Aug 2026 16:41-17:18 PKT | Passed=4 Failed=0 Skipped=0 (~37 min)")
    pdf.kv(
        "Traveller rules",
        "Alpha names only; Special Request dropdowns filled; Pakistan (+92); PK mobile 3001234567",
    )

    pdf.section("2. Suite steps (overview)")
    pdf.status_line("Step 01 Login / Home", "PASSED")
    pdf.bullet("Launch app, login or reuse session, dismiss permission/biometric dialogs, reach Home.")
    pdf.status_line("Step 02 Discover plans 1-6", "PASSED")
    pdf.bullet("Search 6 months ahead; open Pay in Installment; keep plans 1-6; return Home.")
    pdf.status_line("Step 03 Cheapest x plans 1-6", "PASSED (green run)")
    pdf.bullet(
        "For each N-month plan: search with N months ahead -> Cheapest -> Installment -> "
        "select N month -> Terms -> Summary -> Traveller -> Continue/end."
    )
    pdf.status_line("Step 04 Fastest + 6-month", "PASSED (green run)")
    pdf.bullet("Search 6 months ahead -> Fastest -> Installment -> 6 month -> Terms -> Proceed With Query -> Traveller.")
    pdf.body(
        "Green-run durations: Step01 18.0s | Step02 149.9s | Step03 1714.5s | Step04 324.7s | Total ~37m 13s."
    )

    pdf.section("3. Step 01 - Login / reach Home (detail)")
    pdf.bullet("Appium session started on connected device")
    pdf.bullet("Package launched (already installed; no APK reinstall)")
    pdf.bullet("Login / Home path completed")
    pdf.bullet("Notifications Allowed / biometric Cancel handled when shown")
    pdf.status_line("Outcome", "PASSED")

    pdf.section("4. Step 02 - Discover installment plans (detail)")
    pdf.bullet("Origin: London | Destination: Islamabad | Trip: Return")
    pdf.bullet("Search horizon: 6 months ahead (example: 12 Feb 2027 -> 13 Mar 2027)")
    pdf.bullet("Blue Pay in Installment confirmed beside Full Payment")
    pdf.bullet("Cheapest applied; installment sheet opened; plans discovered")
    pdf.bullet("Plans retained: 1 month through 6 month")
    pdf.bullet("Navigate back to Home for booking loops")
    pdf.status_line("Outcome", "PASSED")

    pdf.add_page()
    pdf.section("5. Step 03 - Cheapest installment cases by month")
    pdf.body("Each case is a full booking path through Traveller Information.")

    months = [
        ("3.1", "1 month", "~12 Sep 2026 -> 11 Oct 2026", "1 month ahead"),
        ("3.2", "2 month", "~12 Oct 2026 -> 10 Nov 2026", "2 months ahead"),
        ("3.3", "3 month", "~12 Nov 2026 -> 11 Dec 2026", "3 months ahead"),
        ("3.4", "4 month", "~12 Dec 2026 -> 10 Jan 2027", "4 months ahead"),
        ("3.5", "5 month", "~12 Jan 2027 -> 10 Feb 2027", "5 months ahead"),
        ("3.6", "6 month", "12 Feb 2027 -> 13 Mar 2027", "6 months ahead"),
    ]
    for num, plan, dates, note in months:
        pdf.set_x(pdf.l_margin)
        pdf.set_font("Helvetica", "B", 11)
        pdf.multi_cell(0, 7, f"{num}  {plan}  ({note})")
        pdf.bullet(f"Search dates: {dates}")
        pdf.bullet(
            "Pipeline: Home -> Search -> Results -> Cheapest -> Pay in Installment -> "
            "Select plan -> Accept Terms -> Continue -> Price Summary / Proceed -> Traveller -> Continue/end"
        )
        pdf.bullet(
            "Traveller: Muqadas Shoaib; First=Muqadas Last=Shoaib (letters only); "
            "Special Requests Seat/Meal/Service selected; country +92 Pakistan; "
            "mobile 3001234567; How to Contact + Contact Time set"
        )
        pdf.status_line("Status", "INCLUDED")
        pdf.ln(1)

    pdf.section("6. Traveller Information checklist (each booking)")
    pdf.bullet("Saved traveller dropdown: select name once (Muqadas Shoaib)")
    pdf.bullet("First / Last name: alphabetic only - digits cleared if mistyped into name")
    pdf.bullet("Special Request dropdowns: Seat Preference, Meal Request, Special Service")
    pdf.bullet("Contact country code: Pakistan (+92)")
    pdf.bullet("Mobile number: Pakistan local digits (3001234567) in Mobile field only")
    pdf.bullet("Email: from Sign-In credentials")
    pdf.bullet("How to Contact / Contact Time dropdowns selected")
    pdf.bullet("Continue once to leave Traveller screen")

    pdf.add_page()
    pdf.section("7. Step 04 - Fastest + 6-month installment (detail)")
    pdf.bullet("Return to Home / search form")
    pdf.bullet("Search London->Islamabad with 6 months ahead")
    pdf.bullet("Apply Fastest filter")
    pdf.bullet("Open Pay in Installment; select 6 month plan")
    pdf.bullet("Accept Terms; Continue")
    pdf.bullet("Price Summary CTA: Proceed With Query (installment path)")
    pdf.bullet("Complete Traveller (same checklist as above)")
    pdf.bullet("Continue / end flow")
    pdf.status_line("Outcome (green run)", "PASSED")

    pdf.section("8. Pipeline chips (every successful booking)")
    for c in [
        "Home",
        "Search",
        "Results",
        "Filter (Cheapest/Fastest)",
        "Pay in Installment",
        "Select N-month plan",
        "Terms checkbox",
        "Continue",
        "Price Summary",
        "Proceed with payment / Proceed With Query",
        "Traveller fill",
        "Continue/end",
    ]:
        pdf.bullet(c)

    pdf.section("9. Live Traveller fix evidence (current re-run sample)")
    pdf.bullet("First name set to alphabetic: Muqadas")
    pdf.bullet("Last name set to alphabetic: Shoaib")
    pdf.bullet("Seat Preference selected; Meal Request selected; Special Service selected")
    pdf.bullet("Selected Pakistan country code (+92)")
    pdf.bullet("Mobile entered with Pakistan (+92): 3001234567")
    pdf.bullet("How to Contact / Contact Time selected")

    pdf.section("10. Related artifacts")
    pdf.bullet("reports/TravelHouse_Step_By_Step_Execution_Report.html")
    pdf.bullet("reports/TravelHouse_Installment_Months_Execution_Report.html")
    pdf.bullet("reports/TravelHouse_AppBehaviour_PassFail_Report.html")
    pdf.bullet("reports/TravelHouse_FlightBooking_Summary.html")
    pdf.bullet("This PDF: reports/TravelHouse_Step_By_Step_Execution_Report.pdf")

    pdf.section("11. Notes")
    pdf.bullet("1-month plan is included (suite MIN_PLAN_MONTHS=1).")
    pdf.bullet("Earlier flakes (SDK path, UiAutomator2 crash, Search Flight after calendar) fixed in helpers.")
    pdf.bullet("Local suite only - not pushed to git unless requested.")

    OUT.parent.mkdir(parents=True, exist_ok=True)
    pdf.output(str(OUT))
    print(str(OUT))


if __name__ == "__main__":
    build()
