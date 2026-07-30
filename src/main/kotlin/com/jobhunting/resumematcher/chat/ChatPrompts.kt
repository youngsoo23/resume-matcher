package com.jobhunting.resumematcher.chat

object ChatPrompts {
    fun systemPrompt(resume: String, jobDescription: String): String = """
        You are an expert resume coach helping a candidate tailor their resume to a specific job posting.
        Ground every suggestion in the resume and job description below, and in the conversation history.
        When you propose a resume revision, return the COMPLETE updated resume text in `updatedResume` —
        never a partial diff or excerpt. If you are only discussing or answering a question without
        proposing a change, omit `updatedResume`.

        ## Current resume
        $resume

        ## Job description
        $jobDescription
    """.trimIndent()

    fun resumeHtmlFormatPrompt(resume: String): String = """
        Reformat the resume below into a single HTML fragment using EXACTLY this structure and these class names
        (no `<html>`, `<head>`, or `<body>` tags — just the fragment starting at `<div class="resume">`). Only
        reorganize the existing content into this shape; do not invent facts, companies, dates, or skills that
        aren't in the source resume. Omit any section the source resume doesn't have (e.g. skip `.skill-tags` if
        there's no skills section). Repeat `<div class="entry">` once per company/role or per school, in the
        order they appear in the source.

        ```html
        <div class="resume">
          <h1 class="name">Full name (include an English/romanized name too if the source has one)</h1>
          <div class="contact">phone · email · other contact links, separated by " · "</div>

          <p class="summary">Intro paragraph, if the source has one</p>
          <ul class="highlights">
            <li>Key highlight bullet, if the source has intro bullets</li>
          </ul>

          <section class="section">
            <h2>경력 (총 경력 기간, if stated)</h2>
            <div class="entry">
              <div class="entry-header">
                <span class="entry-title">Company — Role</span>
                <span class="entry-period">Period · employment type</span>
              </div>
              <div class="entry-body">
                <h3>주요 업무</h3>
                <ul><li>Task bullet</li></ul>
                <h3>성과</h3>
                <ul><li>Achievement bullet</li></ul>
                <p class="tech-line"><strong>Tech:</strong> stack items separated by " · "</p>
              </div>
            </div>
          </section>

          <section class="section">
            <h2>학력</h2>
            <div class="entry">
              <div class="entry-header">
                <span class="entry-title">School name</span>
                <span class="entry-period">Period</span>
              </div>
              <p>Degree / major description</p>
            </div>
          </section>

          <section class="section">
            <h2>스킬</h2>
            <div class="skill-tags">
              <span class="skill-tag">Skill name</span>
            </div>
          </section>

          <section class="section">
            <h2>언어</h2>
            <p>Language — level</p>
          </section>

          <section class="section">
            <h2>링크</h2>
            <p><a href="URL">Label</a></p>
          </section>
        </div>
        ```

        ## Source resume
        $resume
    """.trimIndent()
}
