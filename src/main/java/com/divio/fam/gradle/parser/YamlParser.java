package com.divio.fam.gradle.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class YamlParser<AST> {
    private final ObjectMapper objectMapper;
    private final ValidatorFactory validatorFactory;
    private final Class<AST> astClass;

    public YamlParser(Class<AST> astClass) {
        this.objectMapper = new ObjectMapper(new YAMLFactory());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.validatorFactory = Validation.buildDefaultValidatorFactory();
        this.astClass = astClass;
    }

    public AST parse(String text) throws YamlParseException {
        AST ast;

        try {
            ast = objectMapper.readValue(text, astClass);
        } catch (JsonProcessingException e) {
            throw new YamlParseException(e.getMessage());
        }

        Set<ConstraintViolation<AST>> validationResult = validatorFactory.getValidator().validate(ast);

        if (validationResult.isEmpty()) {
            return ast;
        }

        String errorMessage = validationResult.stream()
                .map(cv -> cv.getPropertyPath() + " " + cv.getMessage())
                .collect(Collectors.joining("\n"));

        throw new YamlParseException(errorMessage);
    }

    public AST parse(final List<String> lines) throws YamlParseException {
        String joined = String.join("\n", lines);
        return parse(joined);
    }

    public AST parse(final File file) throws YamlParseException, IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        return parse(lines);
    }

    public void write(final AST ast, final Writer writer) throws IOException {
        // TODO validate the ast we're trying to write
        objectMapper.writeValue(writer, ast);
    }

    public void write(final AST ast, final File file) throws IOException {
        try (FileWriter fileWriter = new FileWriter(file)) {
            objectMapper.writeValue(fileWriter, ast);
        }
    }

    public String writeToString(final AST ast) {
        try {
            return objectMapper.writeValueAsString(ast);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not convert AST to String", e);
        }
    }
}
