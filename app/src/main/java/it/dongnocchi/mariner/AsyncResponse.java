package it.dongnocchi.mariner;

/**
 * Created by Giovanni on 29/06/2015.
 * questa interfaccia e utile come "connettore" tra le async task e i thread principale. se si implementa il metodo
 * processFinish quando viene chiamata process finish riceve la stringa dalla relativa async task,
 */
public interface AsyncResponse {

    void processFinish(String output);
}