import { useState } from "react";
import "./App.css";

function App() {
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [sourceCode, setSourceCode] = useState("");

  // =====================================================
  // EXPLAIN ISSUE STATES
  // =====================================================

  const [explanation, setExplanation] = useState("");
  const [explaining, setExplaining] = useState(false);
  const [selectedIssue, setSelectedIssue] = useState(null);
  
  // =====================================================
  // SUGGEST FIX STATES
  // =====================================================

  const [fix, setFix] = useState("");
  const [fixing, setFixing] = useState(false);
  const [fixIssue, setFixIssue] = useState(null);

  // =====================================================
  // SOURCE CODE LINES
  // =====================================================

  const codeLines = sourceCode
    ? sourceCode.split(/\r?\n/)
    : [];

  // =====================================================
  // FILE SELECTION
  // =====================================================

  const handleFileChange = async (event) => {
    const selectedFile = event.target.files?.[0];

    setResult(null);
    setError("");
    setSourceCode("");

    setExplanation("");
    setSelectedIssue(null);

    setFix("");
    setFixIssue(null);

    if (!selectedFile) {
      setFile(null);
      return;
    }

    if (!selectedFile.name.toLowerCase().endsWith(".java")) {
      setFile(null);
      setError("Please select a Java (.java) file.");
      return;
    }

    setFile(selectedFile);

    try {
      const code = await selectedFile.text();
      setSourceCode(code);
    } catch (err) {
      console.error("Unable to read Java file:", err);
      setError("Unable to read the selected Java file.");
    }
  };

  // =====================================================
  // UPLOAD + ANALYZE
  // =====================================================

  const handleUpload = async () => {
    if (!file) {
      setError("Please select a Java file first.");
      return;
    }

    setLoading(true);
    setError("");
    setResult(null);

    setExplanation("");
    setSelectedIssue(null);

    setFix("");
    setFixIssue(null);

    const formData = new FormData();

    // Must match:
    // @RequestParam("file")
    formData.append("file", file);

    console.log("Uploading:", file.name);

    try {
      const response = await fetch(
        "https://code-review-assistant-backend-m2wv.onrender.com/api/files/upload",
        {
          method: "POST",
          body: formData,
        }
      );

      const contentType =
        response.headers.get("content-type") || "";

      let data;

      if (contentType.includes("application/json")) {
        data = await response.json();
      } else {
        const text = await response.text();

        data = {
          message: text,
        };
      }

      console.log("Backend response:", data);

      if (!response.ok) {
        throw new Error(
          data?.message ||
            data?.error ||
            "Code analysis failed."
        );
      }

      setResult(data);
    } catch (err) {
      console.error("Upload error:", err);

      if (err instanceof TypeError) {
        setError(
          "Unable to connect to the backend. Make sure Spring Boot is running on port 8080."
        );
      } else {
        setError(
          err?.message ||
            "Code analysis failed."
        );
      }
    } finally {
      setLoading(false);
    }
  };

  // =====================================================
  // GET ISSUE LINE
  // =====================================================

  const getIssueLine = (issue) => {
    if (
      issue?.line !== undefined &&
      issue?.line !== null
    ) {
      const line = Number(issue.line);

      if (!Number.isNaN(line)) {
        return line;
      }
    }

    if (
      issue?.lineNumber !== undefined &&
      issue?.lineNumber !== null
    ) {
      const line = Number(issue.lineNumber);

      if (!Number.isNaN(line)) {
        return line;
      }
    }

    return null;
  };

  // =====================================================
  // GET ISSUES FOR SOURCE LINE
  // =====================================================

  const getIssuesForLine = (lineNumber) => {
    const issues = Array.isArray(result?.issues)
      ? result.issues
      : [];

    return issues.filter(
      (issue) =>
        getIssueLine(issue) === lineNumber
    );
  };

  // =====================================================
  // GET CODE FOR ISSUE
  // =====================================================

  const getCodeForIssue = (issue) => {
    const lineNumber = getIssueLine(issue);

    // First try the exact problematic line
    if (
      lineNumber !== null &&
      lineNumber > 0 &&
      lineNumber <= codeLines.length
    ) {
      const line = codeLines[lineNumber - 1];

      if (line && line.trim() !== "") {
        return line;
      }
    }

    // Otherwise send complete source code
    return sourceCode || "";
  };

  // =====================================================
  // EXPLAIN ISSUE
  // =====================================================

  const handleExplainIssue = async (issue) => {
    if (!issue) {
      return;
    }

    // Close fix panel
    setFix("");
    setFixIssue(null);

    setSelectedIssue(issue);
    setExplanation("");
    setExplaining(true);
    setError("");

    const lineNumber = getIssueLine(issue);
    const code = getCodeForIssue(issue);

    console.log("Explaining issue:", issue);
    console.log("Line:", lineNumber);
    console.log("Code:", code);

    try {
      const response = await fetch(
        "https://code-review-assistant-backend-m2wv.onrender.com/api/review/explain",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            issue:
              issue?.message ||
              "Code issue detected.",

            code: code || "",
          }),
        }
      );

      const contentType =
        response.headers.get("content-type") || "";

      let data;

      if (contentType.includes("application/json")) {
        data = await response.json();
      } else {
        const text = await response.text();

        data = {
          explanation: text,
        };
      }

      console.log(
        "Explanation response:",
        data
      );

      if (!response.ok) {
        throw new Error(
          data?.message ||
            data?.error ||
            "Unable to explain this issue."
        );
      }

      setExplanation(
        data?.explanation ||
          data?.message ||
          "No explanation was returned."
      );
    } catch (err) {
      console.error(
        "Explain issue error:",
        err
      );

      setExplanation(
        err?.message ||
          "Unable to get an explanation from the backend."
      );
    } finally {
      setExplaining(false);
    }
  };

  // =====================================================
  // SUGGEST FIX
  // =====================================================

  const handleSuggestFix = async (issue) => {
    if (!issue) {
      return;
    }

    // Close explanation panel
    setExplanation("");
    setSelectedIssue(null);

    setFixIssue(issue);
    setFix("");
    setFixing(true);
    setError("");

    const lineNumber = getIssueLine(issue);
    const code = getCodeForIssue(issue);

    console.log("Generating fix for:", issue);
    console.log("Line:", lineNumber);
    console.log("Code:", code);

    try {
      const response = await fetch(
        "https://code-review-assistant-backend-m2wv.onrender.com/api/review/suggest-fix",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            issue:
              issue?.message ||
              "Code issue detected.",

            code: code || "",
          }),
        }
      );

      const contentType =
        response.headers.get("content-type") || "";

      let data;

      if (contentType.includes("application/json")) {
        data = await response.json();
      } else {
        const text = await response.text();

        data = {
          fix: text,
        };
      }

      console.log(
        "Suggested fix response:",
        data
      );

      if (!response.ok) {
        throw new Error(
          data?.message ||
            data?.error ||
            "Unable to generate a suggested fix."
        );
      }

      setFix(
        data?.fix ||
          data?.suggestion ||
          data?.message ||
          "No suggested fix was returned."
      );
    } catch (err) {
      console.error(
        "Suggest fix error:",
        err
      );

      setFix(
        err?.message ||
          "Unable to generate a suggested fix."
      );
    } finally {
      setFixing(false);
    }
  };

  // =====================================================
  // CLOSE EXPLANATION
  // =====================================================

  const closeExplanation = () => {
    setExplanation("");
    setSelectedIssue(null);
    setExplaining(false);
  };

  // =====================================================
  // CLOSE FIX
  // =====================================================

  const closeFix = () => {
    setFix("");
    setFixIssue(null);
    setFixing(false);
  };

  // =====================================================
  // ISSUES
  // =====================================================

  const issues = Array.isArray(result?.issues)
    ? result.issues
    : [];

  // =====================================================
  // ISSUE COUNTS
  // =====================================================

  const highCount = issues.filter(
    (issue) =>
      issue?.severity?.toUpperCase() === "HIGH"
  ).length;

  const mediumCount = issues.filter(
    (issue) =>
      issue?.severity?.toUpperCase() === "MEDIUM"
  ).length;

  const lowCount = issues.filter(
    (issue) =>
      issue?.severity?.toUpperCase() === "LOW"
  ).length;

  // =====================================================
  // SCORE
  // =====================================================

  const score =
    typeof result?.score === "number"
      ? result.score
      : 0;

  // =====================================================
  // UI
  // =====================================================

  return (
    <div className="app">

      {/* =================================================
          HEADER
      ================================================= */}

      <header className="header">

        <div>
          <h1>
            Code Review Assistant
          </h1>

          <p>
            AI-powered code analysis that detects
            silent bugs, code smells and potential
            improvements.
          </p>
        </div>

        <div className="status">
          <span className="status-dot"></span>
          Backend Online
        </div>

      </header>

      {/* =================================================
          MAIN
      ================================================= */}

      <main className="container">

        {/* =================================================
            UPLOAD CARD
        ================================================= */}

        <section className="upload-card">

          <div className="upload-icon">
            ↑
          </div>

          <h2>
            Upload Java Code
          </h2>

          <p>
            Upload a <strong>.java</strong> file to
            analyze your code using static analysis
            and Smart Review.
          </p>

          <label className="file-button">

            Choose Java File

            <input
              type="file"
              accept=".java"
              onChange={handleFileChange}
            />

          </label>

          {file && (
            <div className="selected-file">
              📄 {file.name}
            </div>
          )}

          <button
            className="analyze-button"
            onClick={handleUpload}
            disabled={
              loading || !file
            }
          >
            {loading
              ? "Analyzing..."
              : "Analyze Code"}
          </button>

          {error && (
            <div className="error">
              {error}
            </div>
          )}

        </section>

        {/* =================================================
            RESULTS
        ================================================= */}

        {result && (

          <section className="results">

            {/* =================================================
                RESULT HEADER
            ================================================= */}

            <div className="result-header">

              <div>

                <h2>
                  Analysis Results
                </h2>

                <p>
                  {result?.fileName ||
                    file?.name}
                </p>

              </div>

              <div className="success-badge">
                ✓ Analysis Complete
              </div>

            </div>

            {/* =================================================
                SCORE
            ================================================= */}

            <div className="score-card">

              <div className="score-title">
                CODE QUALITY SCORE
              </div>

              <div className="score-value">
                {score}

                <span>
                  /100
                </span>
              </div>

              <div className="score-subtitle">
                {issues.length} issue
                {issues.length !== 1
                  ? "s"
                  : ""}{" "}
                detected
              </div>

            </div>

            {/* =================================================
                ISSUE SUMMARY
            ================================================= */}

            <div className="issue-summary">

              <div className="summary-card high-card">

                <span className="summary-number">
                  {highCount}
                </span>

                <span className="summary-label">
                  HIGH
                </span>

              </div>

              <div className="summary-card medium-card">

                <span className="summary-number">
                  {mediumCount}
                </span>

                <span className="summary-label">
                  MEDIUM
                </span>

              </div>

              <div className="summary-card low-card">

                <span className="summary-number">
                  {lowCount}
                </span>

                <span className="summary-label">
                  LOW
                </span>

              </div>

            </div>

            {/* =================================================
                DETECTED ISSUES
            ================================================= */}

            {issues.length > 0 && (

              <div className="issues-section">

                <h3>
                  Detected Issues
                </h3>

                {issues.map(
                  (issue, index) => {

                    const lineNumber =
                      getIssueLine(issue);

                    const severity =
                      issue?.severity
                        ?.toLowerCase() || "";

                    const isSelected =
                      selectedIssue === issue;

                    const isFixSelected =
                      fixIssue === issue;

                    return (

                      <div
                        className={`issue ${
                          isSelected ||
                          isFixSelected
                            ? "selected-issue"
                            : ""
                        }`}
                        key={
                          `${index}-${issue?.message || "issue"}`
                        }
                      >

                        {/* ISSUE TOP */}

                        <div className="issue-top">

                          <span
                            className={`severity ${severity}`}
                          >
                            {issue?.severity ||
                              "UNKNOWN"}
                          </span>

                          <span className="category">
                            {issue?.category ||
                              "GENERAL"}
                          </span>

                          {lineNumber !== null &&
                            lineNumber > 0 && (

                              <span className="line-number">
                                Line {lineNumber}
                              </span>

                            )}

                        </div>

                        {/* ISSUE MESSAGE */}

                        <h4>
                          {issue?.message ||
                            "No description available."}
                        </h4>

                        {/* SUGGESTION */}

                        {issue?.suggestion && (

                          <p className="suggestion">
                            💡{" "}
                            {issue.suggestion}
                          </p>

                        )}

                        {/* =================================================
                            ISSUE ACTIONS
                        ================================================= */}

                        <div className="issue-actions">

                          {/* EXPLAIN */}

                          <button
                            className="explain-button"
                            onClick={() =>
                              handleExplainIssue(
                                issue
                              )
                            }
                            disabled={
                              explaining ||
                              fixing
                            }
                          >
                            {explaining &&
                            selectedIssue === issue
                              ? "Explaining..."
                              : "Explain Issue"}
                          </button>

                          {/* SUGGEST FIX */}

                          <button
                            className="fix-button"
                            onClick={() =>
                              handleSuggestFix(
                                issue
                              )
                            }
                            disabled={
                              fixing ||
                              explaining
                            }
                          >
                            {fixing &&
                            fixIssue === issue
                              ? "Generating Fix..."
                              : "Suggest Fix"}
                          </button>

                        </div>

                      </div>

                    );
                  }
                )}

              </div>

            )}

            {/* =================================================
                EXPLANATION PANEL
            ================================================= */}

            {selectedIssue && (

              <div className="explanation-card">

                <div className="explanation-header">

                  <div>

                    <h3>
                      Issue Explanation
                    </h3>

                    <p>
                      {selectedIssue?.message}
                    </p>

                  </div>

                  <button
                    className="close-button"
                    onClick={
                      closeExplanation
                    }
                  >
                    ✕
                  </button>

                </div>

                {explaining ? (

                  <div className="explanation-loading">

                    <div className="loading-spinner"></div>

                    <p>
                      Analyzing the issue...
                    </p>

                  </div>

                ) : (

                  <pre className="explanation-text">
                    {explanation ||
                      "No explanation available."}
                  </pre>

                )}

              </div>

            )}

            {/* =================================================
                SUGGESTED FIX PANEL
            ================================================= */}

            {fixIssue && (

              <div className="fix-card">

                <div className="explanation-header">

                  <div>

                    <h3>
                      Suggested Fix
                    </h3>

                    <p>
                      {fixIssue?.message}
                    </p>

                  </div>

                  <button
                    className="close-button"
                    onClick={closeFix}
                  >
                    ✕
                  </button>

                </div>

                {fixing ? (

                  <div className="explanation-loading">

                    <div className="loading-spinner"></div>

                    <p>
                      Generating a suggested fix...
                    </p>

                  </div>

                ) : (

                  <pre className="explanation-text">
                    {fix ||
                      "No suggested fix available."}
                  </pre>

                )}

              </div>

            )}

            {/* =================================================
                NO ISSUES
            ================================================= */}

            {issues.length === 0 && (

              <div className="card">

                <h3>
                  ✓ No Issues Detected
                </h3>

                <p>
                  The uploaded Java file passed
                  the current static analysis rules.
                </p>

              </div>

            )}

            {/* =================================================
                SOURCE CODE
            ================================================= */}

            {sourceCode && (

              <div className="card source-card">

                <div className="source-header">

                  <div>

                    <h3>
                      Source Code
                    </h3>

                    <p>
                      Problematic lines are
                      highlighted.
                    </p>

                  </div>

                  <span className="source-file">
                    📄 {file?.name}
                  </span>

                </div>

                {/* CODE VIEWER */}

                <div className="code-viewer">

                  {codeLines.map(
                    (line, index) => {

                      const lineNumber =
                        index + 1;

                      const lineIssues =
                        getIssuesForLine(
                          lineNumber
                        );

                      const hasIssue =
                        lineIssues.length > 0;

                      return (

                        <div
                          key={lineNumber}
                          className={`code-line ${
                            hasIssue
                              ? "problem-line"
                              : ""
                          }`}
                          title={
                            hasIssue
                              ? lineIssues
                                  .map(
                                    (issue) =>
                                      issue?.message ||
                                      "Issue detected"
                                  )
                                  .join(" | ")
                              : ""
                          }
                        >

                          <span className="code-line-number">
                            {lineNumber}
                          </span>

                          <span className="code-content">
                            {line || " "}
                          </span>

                          {hasIssue && (

                            <span className="code-warning">
                              ⚠
                            </span>

                          )}

                        </div>

                      );
                    }
                  )}

                </div>

              </div>

            )}

            {/* =================================================
                FULL REPORT
            ================================================= */}

            <div className="card">

              <h3>
                Code Review Report
              </h3>

              <pre className="report">
                {result?.report ||
                  "No report available."}
              </pre>

            </div>

          </section>

        )}

      </main>

    </div>
  );
}

export default App;