<%-- 
    Document   : processOverview
    Created on : 22-apr-2010, 19:31:42
    Author     : Erik van de Pol
--%>
<%@include file="/pages/commons/taglibs.jsp" %>

<style type="text/css">
    #processesList { width: 50%; margin-top: 10px; margin-bottom: 10px }
    #processesList .ui-button { margin: 3px; display: block; text-align: left; background: #eeeeee; color: black }
    #processesList .ui-state-hover { background: #FECA40; }
    #processesList .ui-state-active { background: #f2d81c; }
</style>

<script type="text/javascript">
    $(function() {
        $("#processesList").buttonset();

        $("#newProcess").button();
        $("#editProcess").button();
        $("#deleteProcess").button();
        $("#executeProcess").button();

        $("#newProcess").click(function() {
            newProcessDialog = $("#newProcessContainer").dialog({
                title: "Nieuw Proces...", // TODO: localization
                width: 800,
                height: 600,
                modal: true,
                close: function() {
                    log("newProcessDialog closing");
                    if ($("#newProcessWizardForm")) {
                        $("#newProcessWizardForm").formwizard("destroy");
                    }
                    newProcessDialog.dialog("destroy");
                    // volgende regel heel belangrijk!!
                    newProcessDialog.remove();
                }
            });
            
            // ajax:
            invoke("#processForm", "new_", "#newProcessContainer");
            
            return false;
        });

        $("#executeProcess").click(function() {
            $("#executeContainer").dialog({
                title: "Process uitvoeren...", // TODO: localization
                width: 800,
                height: 600,
                modal: true,
                buttons: {
                    "Annuleren": function() { // TODO: localize
                        // dit goed checken!!
                        $(this).dialog("close");
                        //$(this).dialog("destroy");
                    }
                },
                close: function() {
                    // dit goed checken!!
                    //$(this).dialog("close");
                    //$(this).dialog("destroy");
                }
            });
            
            // ajax:
            invoke("#processForm", "execute", "#executeContainer");

            return false;
        });
    });

</script>

<stripes:form id="processForm" beanclass="nl.b3p.datastorelinker.gui.stripes.ProcessOverviewAction">
    <stripes:label for="processesOverview.text.overview"/>:

    <div id="processesList">
        <c:forEach var="process" items="${actionBean.processes}" varStatus="status">
            <stripes:radio id="process${status.index}" name="processId" value="${process.id}"/>
            <stripes:label for="process${status.index}"><c:out value="${process.name}"/></stripes:label>
        </c:forEach>
    </div>

    <div id="buttonPanel">
        <stripes:button id="newProcess" name="new_"/>
        <stripes:button id="editProcess" name="edit"/>
        <stripes:button id="deleteProcess" name="delete"/>
        <stripes:button id="executeProcess" name="execute"/>
    </div>
        
</stripes:form>

<div id="executeContainer"/>

<div id="newProcessContainer"/>
