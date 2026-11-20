from pathlib import Path
import requests
import pandas as pd
import matplotlib.pyplot as plt

API = "http://localhost:8080/api/reports/reading-patterns"
OUTPUT = Path(__file__).parent / "output"
OUTPUT.mkdir(exist_ok=True)

def main():
    try:
        response = requests.get(API, timeout=10)
        response.raise_for_status()
        records = response.json()
    except requests.RequestException as exc:
        print("Could not reach the Java backend. Start it on port 8080 first.")
        print(exc)
        return

    if not records:
        print("No borrowing transactions are available yet.")
        return

    df = pd.DataFrame(records)
    df["issueDate"] = pd.to_datetime(df["issueDate"], errors="coerce")
    df["category"] = df["category"].fillna("Uncategorized")
    df["studentName"] = df["studentName"].fillna("Unknown")
    df = df.dropna(subset=["issueDate"])

    print("\nBORROWING DATA SUMMARY")
    print("----------------------")
    print(f"Total transactions: {len(df)}")
    print("\nBooks borrowed by category:")
    print(df["category"].value_counts().to_string())
    print("\nMost active readers:")
    print(df["studentName"].value_counts().head(10).to_string())

    category_counts = df["category"].value_counts()
    category_counts.plot(kind="bar", title="Books Borrowed by Category", xlabel="Category", ylabel="Number of loans")
    plt.tight_layout()
    plt.savefig(OUTPUT / "category_analysis.png")
    plt.close()

    monthly = df.set_index("issueDate").resample("ME").size()
    monthly.plot(kind="line", marker="o", title="Monthly Borrowing Trend", xlabel="Month", ylabel="Number of loans")
    plt.tight_layout()
    plt.savefig(OUTPUT / "monthly_trend.png")
    plt.close()

    df.to_csv(OUTPUT / "cleaned_borrowing_data.csv", index=False)
    print(f"\nAnalysis complete. Charts and cleaned data saved in: {OUTPUT}")

if __name__ == "__main__":
    main()
