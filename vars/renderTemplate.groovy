import groovy.text.StreamingTemplateEngine

def call(input, variables) {
  def engine = new StreamingTemplateEngine()
  return engine.createTemplate(input).make(variables).toString()
}
